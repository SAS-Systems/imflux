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
import sas_systems.imflux.packet.RtpVersion;

/**
 * Represents a control packet. There are five different control packet types (see {@link sas_systems.imflux.packet.rtcp.ControlPacket.Type}).
 * The header is for all control packet types the same:
 * <pre>
 *  0               1               2               3                bytes
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7  bits
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|   RC    | packet type   |            length             | header
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    depents on packet type                     |
 * |                        variable length                        |
 * |                             ....                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <ui>
 * 	<li>V: Version, only supportet: version 2</li>
 * 	<li>P: padding-flag</li>
 *  <li>RC: report counter, number of reports contained by this packet</li>
 *  <li>PT: packet type, see {@link sas_systems.imflux.packet.rtcp.ControlPacket.Type} </li>
 *  <li>length, length of the packed without the header in 32bit-words</li>
 * </ul>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 * @see SenderReportPacket
 * @see ReceiverReportPacket
 * @see SourceDescriptionPacket
 * @see ByePacket
 * @see AppDataPacket
 */
public abstract class ControlPacket {

    // internal vars --------------------------------------------------------------------------------------------------
    protected RtpVersion version;
    protected Type type;

    // constructors ---------------------------------------------------------------------------------------------------
    /**
     * Creates a new empty {@code ControlPacket}.
     * 
     * @param type 
     */
    protected ControlPacket(Type type) {
        this.version = RtpVersion.V2;
        this.type = type;
    }

    // public methods -------------------------------------------------------------------------------------------------
    /**
     * Decodes a control packet from a {@code ChannelBuffer}. The first fields of the RTCP-header is the same for each 
     * control packet. This is done in this method, after that it is delegated to the specified control packet.
	 * 
     * @param buffer bytes to be decoded
     * @return a new {@code ControlPacket} containing all information from the {@code buffer}
     * @throws IllegalArgumentException
     */
    public static ControlPacket decode(ByteBuf buffer) throws IllegalArgumentException {
    	// check buffer size
        if ((buffer.readableBytes() % 4) > 0) {
            throw new IllegalArgumentException("Invalid RTCP packet length: expecting multiple of 4 and got " +
                                               buffer.readableBytes());
        }
        // extract version, padding, innerBlocks and control packet type
        byte b = buffer.readByte();
        RtpVersion version = RtpVersion.fromByte(b);
        if (!version.equals(RtpVersion.V2)) {
            return null;
        }
        boolean hasPadding = (b & 0x20) > 0; // mask: 0010 0000
        byte innerBlocks = (byte) (b & 0x1f); // mask: 0001 1111

        ControlPacket.Type type = ControlPacket.Type.fromByte(buffer.readByte());

        // This length is in 32bit (4byte) words. These first 4 bytes already read, don't count.
        int length = buffer.readShort();
        if (length == 0) {
            return null;
        }

        // No need to pass version downwards, only V2 is supported so subclasses can safely assume V2.
        // I know it's ugly when the superclass knows about the subclasses but since this method is static (and NEEDS
        // to be) the alternative was having this method in a external class. Pointless. 
        switch (type) {
            case SENDER_REPORT:
                return SenderReportPacket.decode(buffer, hasPadding, innerBlocks, length);
            case RECEIVER_REPORT:
                return ReceiverReportPacket.decode(buffer, hasPadding, innerBlocks, length);
            case SOURCE_DESCRIPTION:
                return SourceDescriptionPacket.decode(buffer, hasPadding, innerBlocks, length);
            case BYE:
                return ByePacket.decode(buffer, hasPadding, innerBlocks, length);
            case APP_DATA:
                return null;
            default:
                throw new IllegalArgumentException("Unknown RTCP packet type: " + type);
        }
    }

    public abstract ByteBuf encode(int currentCompoundLength, int fixedBlockSize);

    public abstract ByteBuf encode();

    // getters & setters ----------------------------------------------------------------------------------------------
    public RtpVersion getVersion() {
        return version;
    }

    /**
     * Sets the version header field of the control packet.<br/>
     * <em>Currently only Version 2 is supported!</em>
     * @param version 
     * @see RtpVersion
     */
    public void setVersion(RtpVersion version) {
        if (version != RtpVersion.V2) {
            throw new IllegalArgumentException("Only V2 is supported");
        }
        this.version = version;
    }

    public Type getType() {
        return type;
    }

    // public classes => enumerations ---------------------------------------------------------------------------------
    /**
     * This enumeration represents the packet type of a control packet. 
     * These types are defined in RFC 3605:
     * <table>
     * 	<tr><th>Type</th><th>Abbreviation</th><th>ID (hex)</th><th>ID (dec)</th></tr>
     * 	<tr><td>Sender Report</td><td>SR</td><td>0xC8</td><td>200</td></tr>
     * 	<tr><td>Receiver Report</td><td>RR</td><td>0xC9</td><td>201</td></tr>
     * 	<tr><td>Source Description</td><td>SDES</td><td>0xCA</td><td>202</td></tr>
     * 	<tr><td>Goodbye</td><td>BYE</td><td>0xCB</td><td>203</td></tr>
     * 	<tr><td>Application defined</td><td>APP</td><td>0xCC</td><td>204</td></tr>
     * </table>
     * 
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
     */
    public static enum Type {

        // constants --------------------------------------------------------------------------------------------------
        SENDER_REPORT((byte) 0xc8),
        RECEIVER_REPORT((byte) 0xc9),
        SOURCE_DESCRIPTION((byte) 0xca),
        BYE((byte) 0xcb),
        APP_DATA((byte) 0xcc);

        // internal vars ----------------------------------------------------------------------------------------------
        private byte b;

        // constructors -----------------------------------------------------------------------------------------------
        Type(byte b) {
            this.b = b;
        }

        // public methods ---------------------------------------------------------------------------------------------
        /**
         * Creates a object-representation of the type specified as ID.
         * 
         * @param b the packet type ID
         * @return the packet type as object
         */
        public static Type fromByte(byte b) {
            switch (b) {
                case (byte) 0xc8:
                    return SENDER_REPORT;
                case (byte) 0xc9:
                    return RECEIVER_REPORT;
                case (byte) 0xca:
                    return SOURCE_DESCRIPTION;
                case (byte) 0xcb:
                    return BYE;
                case (byte) 0xcc:
                    return APP_DATA;
                default:
                    throw new IllegalArgumentException("Unknown RTCP packet type: " + b);
            }
        }

        // getters & setters ------------------------------------------------------------------------------------------
        /**
         * 
         * @return the packet type ID as byte
         */
        public byte getByte() {
            return this.b;
        }
        
        /**
         * 
         * @return the packet type ID as integer
         */
        public int getInt() {
        	return new Byte(this.b).intValue();
        }
    }
}
