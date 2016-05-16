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
import io.netty.util.CharsetUtil;

/**
 * This class serves as a factory for {@link SdesChunkItem}s and provides a method to decode these.
 * 
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 * @see SourceDescriptionPacket
 * @see SdesChunkItem
 * @see SdesChunk
 */
public class SdesChunkItems {

    // constants ------------------------------------------------------------------------------------------------------
    public static final SdesChunkItem NULL_ITEM = new SdesChunkItem(SdesChunkItem.Type.NULL, null);

    // public static methods ------------------------------------------------------------------------------------------
    public static SdesChunkItem createNullItem() {
        return NULL_ITEM;
    }

    public static SdesChunkItem createCnameItem(String cname) {
        return new SdesChunkItem(SdesChunkItem.Type.CNAME, cname);
    }

    public static SdesChunkItem createNameItem(String name) {
        return new SdesChunkItem(SdesChunkItem.Type.NAME, name);
    }

    public static SdesChunkItem createEmailItem(String email) {
        return new SdesChunkItem(SdesChunkItem.Type.EMAIL, email);
    }

    public static SdesChunkItem createPhoneItem(String phone) {
        return new SdesChunkItem(SdesChunkItem.Type.PHONE, phone);
    }

    public static SdesChunkItem createLocationItem(String location) {
        return new SdesChunkItem(SdesChunkItem.Type.LOC, location);
    }

    public static SdesChunkItem createToolItem(String tool) {
        return new SdesChunkItem(SdesChunkItem.Type.TOOL, tool);
    }

    public static SdesChunkItem createNoteItem(String note) {
        return new SdesChunkItem(SdesChunkItem.Type.NOTE, note);
    }

    public static SdesChunkPrivItem createPrivItem(String prefix, String value) {
        return new SdesChunkPrivItem(prefix, value);
    }

    /**
     * Decodes a single chunk item.
     * 
     * @param buffer a ByteBuf containing the bytes
     * @return the SdesChunkItem of the proper type (see {@link SdesChunkItem.Type})
     */
    public static SdesChunkItem decode(ByteBuf buffer) {
        SdesChunkItem.Type type = SdesChunkItem.Type.fromByte(buffer.readByte());
        switch (type) {
            case NULL:
                return NULL_ITEM;
            case CNAME:
            case NAME:
            case EMAIL:
            case PHONE:
            case LOC:
            case TOOL:
            case NOTE:
                byte[] value = new byte[buffer.readUnsignedByte()];
                buffer.readBytes(value);
                return new SdesChunkItem(type, new String(value, CharsetUtil.UTF_8));
            case PRIV:
                short valueLength = buffer.readUnsignedByte();
                short prefixLength = buffer.readUnsignedByte();
                // Value length field indicates the length of all that follows this field:
                // Prefix length (1b), Prefix (xb) and Value itself (xb). Thus, the actual value length is equal to
                // the length indicated by this field - 1b (prefix length) and - xb (prefix value).
                value = new byte[valueLength - prefixLength - 1];
                byte[] prefix = new byte[prefixLength];
                buffer.readBytes(prefix);
                buffer.readBytes(value);
                return new SdesChunkPrivItem(new String(prefix, CharsetUtil.UTF_8),
                                             new String(value, CharsetUtil.UTF_8));
            default:
                throw new IllegalArgumentException("Unknown type of SDES chunk: " + type);
        }
    }

    public static ByteBuf encode(SdesChunkItem item) {
        return item.encode();
    }
}
