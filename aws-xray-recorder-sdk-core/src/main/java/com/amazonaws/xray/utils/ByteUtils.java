package com.amazonaws.xray.utils;

public class ByteUtils {
    static final String HEXES = "0123456789ABCDEF";

    /**
     * ref: https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
     *
     * Converts the input byte array into a hexadecimal string.
     * @param raw - Byte array
     * @return String - Hexadecimal representation of the byte array.
     */
    public static String byteArrayToHexString( byte [] raw ) {
        if ( raw == null ) {
            return null;
        }
        final StringBuilder hex = new StringBuilder( 2 * raw.length );
        for ( final byte b : raw ) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
