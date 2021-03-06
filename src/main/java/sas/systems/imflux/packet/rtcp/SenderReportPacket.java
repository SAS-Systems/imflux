/*
 * Copyright 2015 Sebastian Schmidl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sas.systems.imflux.packet.rtcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;

/**
 * A control packet of type SR (sender report):
 * <pre>
 *  0               1               2               3                bytes
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7  bits
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|   RC    | packet type   |            length             | header
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         SSRC of sender                        |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |              NTP timestamp, most significant word             | sender info
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |             NTP timestamp, least significant word             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         RTP timestamp                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                     sender's packet count                     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      sender's octet count                     |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |                 SSRC_1 (SSRC of first source)                 | report block 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | fraction lost |       cumulative number of packets lost       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           extended highest sequence number received           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      interarrival jitter                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         last SR (LSR)                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                   delay since last SR (DLSR)                  |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |                 SSRC_2 (SSRC of second source)                | report block 2
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * :                               ...                             :
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |                  profile-specific extensions                  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 * @see ControlPacket
 * @see ReceiverReportPacket
 * @see SourceDescriptionPacket
 * @see ByePacket
 * @see AppDataPacket
 */
public class SenderReportPacket extends AbstractReportPacket {

    // internal vars --------------------------------------------------------------------------------------------------
    private BigInteger ntpTimestamp;
    private long rtpTimestamp;
    private long senderPacketCount;
    private long senderOctetCount;

    // constructors ---------------------------------------------------------------------------------------------------
    public SenderReportPacket() {
        super(Type.SENDER_REPORT);
        ntpTimestamp = new BigInteger(new byte[1]);
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * Decodes a control packet from a {@code ByteBuf}. This method is called by {@code ControlPacket.decode()}.
     * 
     * @param buffer bytes, which still have to be decoded
     * @param innerBlocks number of reports in this packet
     * @param length remaining 32bit words
     * @return a new {@code SenderReportPacket} containing all information from the {@code buffer}
     */
    public static SenderReportPacket decode(ByteBuf buffer, byte innerBlocks, int length) {
        SenderReportPacket packet = new SenderReportPacket();
        
        packet.setSenderSsrc(buffer.readUnsignedInt());			// reads 4 bytes (one 32bit word) from the buffer
        packet.setNtpTimestamp(									// reads 2x 4bytes (two 32bit words) from the buffer
    		BigInteger.valueOf(buffer.readUnsignedInt())			// read high word
    		.multiply(BigInteger.valueOf(4294967296L))				// multiply by 0x0001 0000 0000 to shift value to
    																//   the right position
    		.add(BigInteger.valueOf(buffer.readUnsignedInt()))		// read low word
        );
        packet.setRtpTimestamp(buffer.readUnsignedInt());		// reads 4 bytes (one 32bit word) from the buffer
        packet.setSenderPacketCount(buffer.readUnsignedInt());	// reads 4 bytes (one 32bit word) from the buffer
        packet.setSenderOctetCount(buffer.readUnsignedInt());	// reads 4 bytes (one 32bit word) from the buffer
        														//   =  24 bytes

        int read = 24;
        for (int i = 0; i < innerBlocks; i++) {
            packet.addReportBlock(ReceptionReport.decode(buffer));
            read += 24; // Each SR/RR block has 24 bytes (6 32bit words)
        }

        // Length is written in 32bit words, not octet count.
        int lengthInOctets = length * 4;
        // (hasPadding == true) check is not done here. RFC respecting implementations will set the padding bit to 1
        // if length of packet is bigger than the necessary to convey the data; therefore it's a redundant check.
        if (read < lengthInOctets) {
            // Skip remaining bytes (used for padding).
            buffer.skipBytes(lengthInOctets - read);
        }

        return packet;
    }

    /**
     * Encodes a {@code SenderReportPacket}.
     * 
     * @param currentCompoundLength only needed for the padding if {@code fixedBlockSize > 0}
     * @param fixedBlockSize set this size if the packet should have a fixed size, otherwise 0
     * @param packet the packet to be encoded
     * @return a {@code ByteBuf} containing the packet as bytes
     */
    public static ByteBuf encode(int currentCompoundLength, int fixedBlockSize, SenderReportPacket packet) {
        // TODO: shorten this method
        if ((currentCompoundLength < 0) || ((currentCompoundLength % 4) > 0)) {
            throw new IllegalArgumentException("Current compound length must be a non-negative multiple of 4");
        }
        if ((fixedBlockSize < 0) || ((fixedBlockSize % 4) > 0)) {
            throw new IllegalArgumentException("Padding modulus must be a non-negative multiple of 4");
        }
        byte[] temp = packet.ntpTimestamp.toByteArray();
        
        /*
         * !! only to be very sure !!
         *    should be found by setNtpTimestamp
         *    
         * If the value specified in packet.ntpTimestamp is near the value range of the 8. byte Java adds a leading byte
         * with the value 0x00 to support two's complement alignment. We have to accept this and ignore the 9. byte as 
         * a NTP timestamp is a unsigned 64-bit number (see https://www.eecis.udel.edu/~mills/y2k.html).
         * 
         * TODO: delete additional range check
         */
        if(temp.length > 8) {
	        if(temp.length > 9 || (temp.length == 9 && temp[0] != 0x00)) {
		    	throw new UnsupportedOperationException("Couldn't encode ntpTimestamp because it's too long!");
		    }
        }
        // copy into new array to allow leading zeros and ensure writing of 8 bytes for ntpTimestamp
        byte[] timestamp = new byte[8];
        for (int i = Math.min(temp.length, timestamp.length); i > 0; i--) {
			timestamp[timestamp.length-i] = temp[temp.length-i];
		}        
        

        // Common header + other fields (sender ssrc, ntp timestamp, rtp timestamp, packet count, octet count) in bytes
        int size = 4 + 24;
        if (packet.reports != null) {
            size += packet.reports.size() * 24;
        }
        ByteBuf buffer;

        // If packet was configured to have padding, calculate padding and add it.
        int padding = 0;
        if (fixedBlockSize > 0) {
            // If padding modulus is > 0 then the padding is equal to:
            // (global size of the compound RTCP packet) mod (block size)
            // Block size alignment might be necessary for some encryption algorithms
            // RFC section 6.4.1
            padding = fixedBlockSize - ((size + currentCompoundLength) % fixedBlockSize);
            if (padding == fixedBlockSize) {
                padding = 0;
            }
        }
        size += padding;

        // Allocate the buffer and write contents
        buffer = Unpooled.buffer(size);
        // First byte: Version (2b), Padding (1b), RR count (5b)
        byte b = packet.getVersion().getByte();
        if (padding > 0) {
            b |= 0x20;
        }
        b |= packet.getReportCount();
        buffer.writeByte(b);
        
        // Second byte: Packet Type
        buffer.writeByte(packet.type.getByte());
        
        // Third and fourth byte: total length of the packet, in multiples of 4 bytes (32bit words) - 1 (header)
        int sizeInOctets = (size / 4) - 1;
        buffer.writeShort(sizeInOctets);
        
        // Next 24 bytes: ssrc, ntp timestamp, rtp timestamp, octet count, packet count
        buffer.writeInt((int) packet.senderSsrc);
        buffer.writeBytes(timestamp); 
        buffer.writeInt((int) packet.rtpTimestamp);
        buffer.writeInt((int) packet.senderPacketCount);
        buffer.writeInt((int) packet.senderOctetCount);
        
        // Payload: report blocks
        if (packet.getReportCount() > 0) {
            for (ReceptionReport block : packet.reports) {
                buffer.writeBytes(block.encode());
            }
        }
        
        // padding if required
        if (padding > 0) {
            // Final bytes: padding
            for (int i = 0; i < (padding - 1); i++) {
                buffer.writeByte(0x00);
            }

            // Final byte: the amount of padding bytes that should be discarded.
            // Unless something's wrong, it will be a multiple of 4.
            buffer.writeByte(padding);
        }

        return buffer;
    }

    // ControlPacket --------------------------------------------------------------------------------------------------
    /**
     * Encodes this {@code SenderReportPacket}.
     * 
     * @param currentCompoundLength only needed for the padding if {@code fixedBlockSize > 0}
     * @param fixedBlockSize set this size if the packet should have a fixed size, otherwise 0
     * @return a {@code ByteBuf} containing this packet as bytes
     */
    @Override
    public ByteBuf encode(int currentCompoundLength, int fixedBlockSize) {
        return SenderReportPacket.encode(currentCompoundLength, fixedBlockSize, this);
    }

    /**
     * Encodes this {@code SenderReportPacket} with standard parameters.
     * 
     * @return a {@code ByteBuf} containing this packet as bytes
     */
    @Override
    public ByteBuf encode() {
        return SenderReportPacket.encode(0, 0, this);
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public BigInteger getNtpTimestamp() {
        return ntpTimestamp;
    }

    public void setNtpTimestamp(BigInteger ntpTimestamp) {
        if ((ntpTimestamp.compareTo(new BigInteger("0")) <= 0) 
        		|| (ntpTimestamp.compareTo(new BigInteger("ffffffffffffffff", 16)) > 0)) {
            throw new IllegalArgumentException("Valid range for NTP timestamp is [0;0xffffffffffffffff]");
        }
        this.ntpTimestamp = ntpTimestamp;
    }

    public long getRtpTimestamp() {
        return rtpTimestamp;
    }

    public void setRtpTimestamp(long rtpTimestamp) {
        if ((rtpTimestamp < 0) || (rtpTimestamp > 0xffffffffL)) {
            throw new IllegalArgumentException("Valid range for RTP timestamp is [0;0xffffffff]");
        }
        this.rtpTimestamp = rtpTimestamp;
    }

    public long getSenderPacketCount() {
        return senderPacketCount;
    }

    public void setSenderPacketCount(long senderPacketCount) {
        if ((senderPacketCount < 0) || (senderPacketCount > 0xffffffffL)) {
            throw new IllegalArgumentException("Valid range for Sender Packet Count is [0;0xffffffff]");
        }
        this.senderPacketCount = senderPacketCount;
    }

    public long getSenderOctetCount() {
        return senderOctetCount;
    }

    public void setSenderOctetCount(long senderOctetCount) {
        if ((senderOctetCount < 0) || (senderOctetCount > 0xffffffffL)) {
            throw new IllegalArgumentException("Valid range for Sender Octet Count is [0;0xffffffff]");
        }
        this.senderOctetCount = senderOctetCount;
    }

    // low level overrides --------------------------------------------------------------------------------------------
    @Override
    public String toString() {
        return "SenderReportPacket{" +
                "senderSsrc=" + this.senderSsrc +
                ", ntpTimestamp=" + this.ntpTimestamp +
                ", rtpTimestamp=" + this.rtpTimestamp +
                ", senderPacketCount=" + this.senderPacketCount +
                ", senderOctetCount=" + this.senderOctetCount +
                ", receptionReports=" + this.reports +
                '}';
    }
}
