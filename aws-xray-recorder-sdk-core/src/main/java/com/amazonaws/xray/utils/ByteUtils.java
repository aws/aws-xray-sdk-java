/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.xray.utils;

import com.amazonaws.xray.internal.RecyclableBuffers;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class ByteUtils {
    static final String HEXES = "0123456789ABCDEF";

    private static final int BYTE_BASE16 = 2;
    private static final String ALPHABET = "0123456789abcdef";
    private static final char[] ENCODING = buildEncodingArray();

    private static char[] buildEncodingArray() {
        char[] encoding = new char[512];
        for (int i = 0; i < 256; ++i) {
            encoding[i] = ALPHABET.charAt(i >>> 4);
            encoding[i | 0x100] = ALPHABET.charAt(i & 0xF);
        }
        return encoding;
    }

    /**
     * ref: https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
     *
     * Converts the input byte array into a hexadecimal string.
     * @param raw - Byte array
     * @return String - Hexadecimal representation of the byte array.
     */
    @Nullable
    public static String byteArrayToHexString(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static String bytesToBase16String(byte[] bytes) {
        char[] dest = RecyclableBuffers.chars(24);
        for (int i = 0; i < 12; i++) {
            byteToBase16(bytes[i], dest, i * BYTE_BASE16);
        }

        return new String(dest, 0, 24);
    }

    public static String numberToBase16String(int hi, long lo) {
        char[] dest = RecyclableBuffers.chars(24);

        byteToBase16((byte) (hi >> 24 & 0xFFL), dest, 0);
        byteToBase16((byte) (hi >> 16 & 0xFFL), dest, BYTE_BASE16);
        byteToBase16((byte) (hi >> 8 & 0xFFL), dest, 2 * BYTE_BASE16);
        byteToBase16((byte) (hi & 0xFFL), dest, 3 * BYTE_BASE16);

        byteToBase16((byte) (lo >> 56 & 0xFFL), dest, 4 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 48 & 0xFFL), dest, 5 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 40 & 0xFFL), dest, 6 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 32 & 0xFFL), dest, 7 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 24 & 0xFFL), dest, 8 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 16 & 0xFFL), dest, 9 * BYTE_BASE16);
        byteToBase16((byte) (lo >> 8 & 0xFFL), dest, 10 * BYTE_BASE16);
        byteToBase16((byte) (lo & 0xFFL), dest, 11 * BYTE_BASE16);

        return new String(dest, 0, 24);
    }

    public static String intToBase16String(long value) {
        char[] dest = RecyclableBuffers.chars(8);
        byteToBase16((byte) (value >> 24 & 0xFFL), dest, 0);
        byteToBase16((byte) (value >> 16 & 0xFFL), dest, BYTE_BASE16);
        byteToBase16((byte) (value >> 8 & 0xFFL), dest, 2 * BYTE_BASE16);
        byteToBase16((byte) (value & 0xFFL), dest, 3 * BYTE_BASE16);
        return new String(dest, 0, 8);
    }

    private static void byteToBase16(byte value, char[] dest, int destOffset) {
        int b = value & 0xFF;
        dest[destOffset] = ENCODING[b];
        dest[destOffset + 1] = ENCODING[b | 0x100];
    }
}
