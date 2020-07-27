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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ByteUtilsTest {

    @Test
    void testHexString() {
        byte[] zeroArray = new byte[16];
        assertThat(ByteUtils.byteArrayToHexString(zeroArray)).isEqualTo("00000000000000000000000000000000");

        byte[] emptyArray = {};
        assertThat(ByteUtils.byteArrayToHexString(emptyArray)).isEqualTo("");

        byte[] zeroByte = {(byte) 0x00};
        assertThat(ByteUtils.byteArrayToHexString(zeroByte)).isEqualTo("00");

        byte[] fullByte = {(byte) 0xFF};
        assertThat(ByteUtils.byteArrayToHexString(fullByte)).isEqualTo("FF");

        byte[] leadingZero = {(byte) 0x0F};
        assertThat(ByteUtils.byteArrayToHexString(leadingZero)).isEqualTo("0F");

        byte[] longLeadingZero = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11};
        assertThat(ByteUtils.byteArrayToHexString(longLeadingZero)).isEqualTo("00000000000011");

        byte[] trailingZero = {(byte) 0x11, (byte) 0x00};
        assertThat(ByteUtils.byteArrayToHexString(trailingZero)).isEqualTo("1100");

        byte[] longTrailingZero = new byte[16];
        longTrailingZero[0] = (byte) 0xFF;
        assertThat(ByteUtils.byteArrayToHexString(longTrailingZero)).isEqualTo("FF000000000000000000000000000000");

        byte[] basicArray =
            {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0xFF, (byte) 0xF0, (byte) 0x0F, (byte) 0xFF};
        assertThat(ByteUtils.byteArrayToHexString(basicArray)).isEqualTo("FFFFFF00FFF00FFF");

        byte[] basicVariedArray =
            {(byte) 0x82, (byte) 0xF2, (byte) 0xAB, (byte) 0xA4, (byte) 0xDE, (byte) 0x15, (byte) 0x19, (byte) 0x11};
        assertThat(ByteUtils.byteArrayToHexString(basicVariedArray)).isEqualTo("82F2ABA4DE151911");
    }
}
