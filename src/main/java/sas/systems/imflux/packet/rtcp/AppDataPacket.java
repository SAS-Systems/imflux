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


/**
 * A control packet of type APP (application-defined).<br/>
 * This packet is for experimental use as new applications are developed. <em>This class isn't completed yet</em>, because
 * there isn't the need for it. You can use this class to extend this library with custom features.
 * <pre>
 *  0               1               2               3                bytes
 *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7  bits
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|   RC    |   PT=BYE=203  |            length             | header
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           SSRC/CSRC                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                          name (ASCII)                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                   application-dependent data                ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 * @see ControlPacket
 * @see SenderReportPacket
 * @see SourceDescriptionPacket
 * @see ReceiverReportPacket
 * @see ByePacket
 */
public class AppDataPacket extends ControlPacket {

    // constructors ---------------------------------------------------------------------------------------------------
    public AppDataPacket(Type type) {
        super(type);
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * This method isn't implemented, because there isn't the need for app data packets yet.
     * 
     * @param currentCompoundLength
     * @param fixedBlockSize
     * @param packet
     * @return null
     */
    public static ByteBuf encode(int currentCompoundLength, int fixedBlockSize, AppDataPacket packet) {
        return null;
    }

    // ControlPacket --------------------------------------------------------------------------------------------------
    /**
     * Not implemented yet.
     */
    @Override
    public ByteBuf encode(int currentCompoundLength, int fixedBlockSize) {
        return encode(currentCompoundLength, fixedBlockSize, this);
    }

    /**
     * Not implemented yet.
     */
    @Override
    public ByteBuf encode() {
        return encode(0, 0, this);
    }
}
