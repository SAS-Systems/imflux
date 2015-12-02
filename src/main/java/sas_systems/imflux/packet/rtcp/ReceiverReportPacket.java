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

package sas_systems.imflux.packet.rtcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


/**
 * A control packet of type RR (receiver report):
 * <pre>
 *  0               1               2               3                bytes
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7  bits
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|   RC    |   PT=RR=201   |            length             | header
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         SSRC of sender                        |
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
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * :                               ...                             :
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |                  profile-specific extensions                  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 * @see ControlPacket
 * @see SenderReportPacket
 * @see SourceDescriptionPacket
 * @see ByePacket
 * @see AppDataPacket
 */
public class ReceiverReportPacket extends AbstractReportPacket {

    // constructors ---------------------------------------------------------------------------------------------------
    public ReceiverReportPacket() {
        super(Type.RECEIVER_REPORT);
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * Decodes a receiver report from a {@code ByteBuf}. This method is called by {@code ControlPacket.decode()}.
     * 
     * @param buffer bytes, which still have to be decoded
     * @param hasPadding indicator for a padding at the end of the packet, which have to be discarded
     * @param innerBlocks number of reports in this packet
     * @param length remaining 32bit words
     * @return a new {@code RecieverReportPacket} containing all information from the {@code buffer}
     */
    public static ReceiverReportPacket decode(ByteBuf buffer, boolean hasPadding, byte innerBlocks, int length) {
        ReceiverReportPacket packet = new ReceiverReportPacket();

        packet.setSenderSsrc(buffer.readUnsignedInt());

        int read = 4;
        for (int i = 0; i < innerBlocks; i++) {
            packet.addReportBlock(ReceptionReport.decode(buffer));
            read += 24; // Each SR/RR block has 24 bytes (6 32bit words)
        }

        // Length is written in 32bit words, not octet count.
        int lengthInOctets = (length * 4);
        // (hasPadding == true) check is not done here. RFC respecting implementations will set the padding bit to 1
        // if length of packet is bigger than the necessary to convey the data; therefore it's a redundant check.
        if (read < lengthInOctets) {
            // Skip remaining bytes (used for padding).
            buffer.skipBytes(lengthInOctets - read);
        }

        return packet;
    }

    /**
     * Encodes a {@code ReceiverReportPacket}.
     * 
     * @param currentCompoundLength only needed for the padding if {@code fixedBlockSize > 0}
     * @param fixedBlockSize set this size if the packet should have a fixed size, otherwise 0
     * @param packet the packet to be encoded
     * @return a {@code ByteBuf} containing the packet as bytes
     */
    public static ByteBuf encode(int currentCompoundLength, int fixedBlockSize, ReceiverReportPacket packet) {
        if ((currentCompoundLength < 0) || ((currentCompoundLength % 4) > 0)) {
            throw new IllegalArgumentException("Current compound length must be a non-negative multiple of 4");
        }
        if ((fixedBlockSize < 0) || ((fixedBlockSize % 4) > 0)) {
            throw new IllegalArgumentException("Padding modulus must be a non-negative multiple of 4");
        }

        // Common header + sender ssrc
        int size = 4 + 4;
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
        
        // Third and fourth byte: total length of the packet, in multiples of 4 bytes (32bit words) - 1
        int sizeInOctets = (size / 4) - 1;
        buffer.writeShort(sizeInOctets);
        
        // Next 4 bytes: sender SSRC
        buffer.writeInt((int) packet.senderSsrc);
        
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
     * Encodes this {@code ReceiverReportPacket}.
     * 
     * @param currentCompoundLength only needed for the padding if {@code fixedBlockSize > 0}
     * @param fixedBlockSize set this size if the packet should have a fixed size, otherwise 0
     * @return a {@code ByteBuf} containing this packet as bytes
     */
    @Override
    public ByteBuf encode(int currentCompoundLength, int fixedBlockSize) {
        return encode(currentCompoundLength, fixedBlockSize, this);
    }

    /**
     * Encodes this {@code  ReceiverReportPacket}.
     * 
     * @return a {@code ByteBuf} containing this packet as bytes
     */
    @Override
    public ByteBuf encode() {
        return encode(0, 0, this);
    }

    // low level overrides --------------------------------------------------------------------------------------------
    @Override
    public String toString() {
        return new StringBuilder()
                .append("ReceiverReportPacket{")
                .append("senderSsrc=").append(this.senderSsrc)
                .append(", receptionReports=").append(this.reports)
                .append('}').toString();
    }
}
