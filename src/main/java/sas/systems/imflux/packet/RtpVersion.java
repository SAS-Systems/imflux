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

package sas.systems.imflux.packet;

/**
 * This enumeration defines the RTP versions. <br/>
 * <em>Currently only version 2 is supported!</em>
 * 
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 * @author <a:mailto="bruno.carvalho@wit-software.com" />Bruno de Carvalho</a>
 */
public enum RtpVersion {

    // constants ------------------------------------------------------------------------------------------------------
    V2((byte) 0x80), // 128
    V1((byte) 0x40), // 64
    V0((byte) 0x00); // 0

    // internal vars --------------------------------------------------------------------------------------------------
    private final byte b;

    // constructors ---------------------------------------------------------------------------------------------------
    private RtpVersion(byte b) {
        this.b = b;
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * Creates a object-representation of the version passed as byte.
     * 
     * @param b the RTP version
     * @return RTP version as a object
     * @throws IllegalArgumentException
     */
    public static RtpVersion fromByte(byte b) throws IllegalArgumentException {
        byte tmp = (byte) (b & 0xc0); // mask: 1100 0000
        // Starts from version 2, which is the most common.
        for (RtpVersion version : values()) {
            if (version.getByte() == tmp) {
                return version;
            }
        }

        throw new IllegalArgumentException("Unknown version for byte: " + b);
    }

    // getters & setters ----------------------------------------------------------------------------------------------
    public byte getByte() {
        return b;
    }
}
