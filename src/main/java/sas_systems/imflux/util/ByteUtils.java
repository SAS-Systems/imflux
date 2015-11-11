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

package sas_systems.imflux.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides methods for byte operations.
 * 
 * @author <a href="http://bruno.biasedbit.com/">Bruno de Carvalho</a>
 * @author <a href="https://github.com/CodeLionX">CodeLionX</a>
 */
public class ByteUtils {

    // constructors ---------------------------------------------------------------------------------------------------
    private ByteUtils() {
    }

    // public static methods ------------------------------------------------------------------------------------------
    /**
     * Hash a string with MD5 algorithm
     *
     * @param toHash Object to be hashed.
     * @return Hashed string.
     */
    public static String hash(Object toHash) {
        String hashString = toHash.toString();
        MessageDigest md;
        
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return hashString;
        }

        md.update(hashString.getBytes(), 0, hashString.length());
        return convertToHex(md.digest());
    }

    /**
     * Converts a byte array to a hex string
     * 
     * @param data byte array to be converted to hex
     * @return a hex string
     */
    public static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        
        for (byte aData : data) {
            int halfbyte = (aData >>> 4) & 0x0F; // shift 4 bits right and mask with 0000 1111 to get hex value of the second nipple(half-byte)
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = aData & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Converts a byte array into a hex string. When the {@code packedPrint} option is <b>not</b> set,
     * the hex-string is returned in a more readable form (each byte as 0xbb).
     * 
     * @param array byte array to be converted
     * @param packedPrint option for no fancy output
     * @return a hex string
     */
    public static String writeArrayAsHex(byte[] array, boolean packedPrint) {
        if (packedPrint) {
            return convertToHex(array);
        }

        StringBuilder builder = new StringBuilder();
        for (byte b : array) {
            builder.append(" 0x");
            String hex = Integer.toHexString(b);
            switch (hex.length()) { 
                case 1:
                    builder.append('0').append(hex);	// append leading zero
                    break;
                case 2:
                    builder.append(hex);
                    break;
                default:
                    builder.append(hex.substring(6, 8));
            }
        }

        return builder.toString();
    }

    /**
     * Converts a hex string into a byte array.
     * 
     * @param hexString the hex string to be converted
     * @return a byte array containing the hex values
     */
    public static byte[] convertHexStringToByteArray(String hexString) {
        if ((hexString.length() % 2) != 0) {
            throw new IllegalArgumentException("Invalid hex string (length % 2 != 0)");
        }

        byte[] array = new byte[hexString.length() / 2];
        for (int i = 0, arrayIndex = 0; i < hexString.length(); i += 2, arrayIndex++) {
            array[arrayIndex] = Integer.valueOf(hexString.substring(i, i + 2), 16).byteValue();
        }

        return array;
    }

    /**
     * Get a byte array in a printable binary form.
     *
     * @param bytes The bytes to be writen.
     * @return A String representation of the bytes.
     */
    public static String writeBits(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            // New line every 4 bytes
            if ((i % 4) == 0) {
                stringBuilder.append("\n");
            }
            // a space between each byte
            stringBuilder.append(writeBits(bytes[i])).append(" ");
        }
        return stringBuilder.toString();
    }

    /**
     * Get a byte in a printable binary form.
     *
     * @param b The byte to be writen.
     * @return A String representation of the byte.
     */
    public static String writeBits(byte b) {
        StringBuffer stringBuffer = new StringBuffer();
        int bit;
        for (int i = 7; i >= 0; i--) {
            bit = (b >>> i) & 0x01;
            stringBuffer.append(bit);
        }
        return stringBuffer.toString();
    }
}
