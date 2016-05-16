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
import io.netty.util.CharsetUtil;

/**
 * This class represents an item of a {@link SdesChunk} from a {@link SourceDescriptionPacket}.
 * 
 * <pre>
 *  0               1               2               3                bytes
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7  bits
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   identifier  |     length    |          content            ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>  
 * 
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 * @see SourceDescriptionPacket
 * @see SdesChunk
 * @see SdesChunkPrivItem
 */
public class SdesChunkItem {

    // internal vars --------------------------------------------------------------------------------------------------
    protected final Type type;
    protected final String value;

    // constructors ---------------------------------------------------------------------------------------------------
    protected SdesChunkItem(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    // public methods -------------------------------------------------------------------------------------------------
    /**
     * Encodes this chunk item.
     * 
     * @return a ByteBuf containing the bytes
     */
    public ByteBuf encode() {
        // Technically, this never happens as you're not allowed to add NULL items to a SdesChunk instance, but...
        if (this.type == Type.NULL) {
            ByteBuf buffer = Unpooled.buffer(1);
            buffer.writeByte(0x00);
            return buffer;
        }

        byte[] valueBytes;
        if (this.value != null) {
            // RFC section 6.5 mandates that this must be UTF8
            // http://tools.ietf.org/html/rfc3550#section-6.5
            valueBytes = this.value.getBytes(CharsetUtil.UTF_8);
        } else {
            valueBytes = new byte[]{};
        }

        if (valueBytes.length > 255) {
            throw new IllegalArgumentException("Content (text) can be no longer than 255 bytes and this has " +
                                               valueBytes.length);
        }

        // Type (1b), length (1b), value (xb)
        ByteBuf buffer = Unpooled.buffer(2 + valueBytes.length);
        buffer.writeByte(this.type.getByte());
        buffer.writeByte(valueBytes.length);
        buffer.writeBytes(valueBytes);

        return buffer;
    }

    // getters & setters ----------------------------------------------------------------------------------------------
    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    // low level overrides --------------------------------------------------------------------------------------------
    @Override
    public String toString() {
        return new StringBuilder()
                .append("SdesChunkItem{")
                .append("type=").append(this.type)
                .append(", value='").append(this.value).append('\'')
                .append('}').toString();
    }

    // public classes -------------------------------------------------------------------------------------------------
    /**
     * Enumeration for chunk item types.
     * These types of chunk items are defined in RFC 3550:
     * <table>
     * 	<tr><th>Name</th><th>Abbreviation</th><th>ID</th></tr>
     * 	<tr><td>Sender Report</td><td>NULL</td><td>0</td></tr>
     * 	<tr><td>Canonical End-Point Identifier</td><td>CNAME</td><td>1</td></tr>
     * 	<tr><td>User Name</td><td>NAME</td><td>2</td></tr>
     * 	<tr><td>Electronic Mail Address</td><td>EMAIL</td><td>3</td></tr>
     * 	<tr><td>Phone Number</td><td>PHONE</td><td>4</td></tr>
     *  <tr><td>Geographic User Location</td><td>LOC</td><td>5</td></tr>
     *  <tr><td>Application or Tool Name</td><td>TOOL</td><td>6</td></tr>
     *  <tr><td>Notice/Status</td><td>NOTE</td><td>7</td></tr>
     *  <tr><td>Private Extensions</td><td>PRIV</td><td>8</td></tr>
     * </table>
     * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
     * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
     */
    public static enum Type {

        // constants --------------------------------------------------------------------------------------------------
        NULL((byte) 0),
        CNAME((byte) 1),
        NAME((byte) 2),
        EMAIL((byte) 3),
        PHONE((byte) 4),
        LOC((byte) 5),
        TOOL((byte) 6),
        NOTE((byte) 7),
        PRIV((byte) 8);

        // internal vars ----------------------------------------------------------------------------------------------

        private final byte b;
        
        // constructors -----------------------------------------------------------------------------------------------
        Type(byte b) {
            this.b = b;
        }

        // public static methods --------------------------------------------------------------------------------------
        /**
         * Creates a object-representation of the SDES chunk item type.
         * 
         * @param b the packet type ID
         * @return the packet type as object
         */
        public static Type fromByte(byte b) {
            switch (b) {
                case 0: return NULL;
                case 1: return CNAME;
                case 2: return NAME;
                case 3: return EMAIL;
                case 4: return PHONE;
                case 5: return LOC;
                case 6: return TOOL;
                case 7: return NOTE;
                case 8: return PRIV;
                default: throw new IllegalArgumentException("Unknown SSRC Chunk Item type: " + b);
            }
        }

        // getters & setters ------------------------------------------------------------------------------------------
        public byte getByte() {
            return b;
        }
    }
}
