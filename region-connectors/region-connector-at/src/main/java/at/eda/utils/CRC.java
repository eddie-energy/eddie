package at.eda.utils;

public class CRC {
    private CRC() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Computes the CRC32 for the given data.
     *
     * @param data the data to compute the CRC32 for
     * @return the CRC32 value
     */
    public static long computeCRC32(byte[] data) {
        var crc32 = new java.util.zip.CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    /**
     * Computes the CRC8 for the given data using the DVB-S2 polynomial.
     *
     * @param data the data to compute the CRC8 for
     * @return the CRC8 value
     */
    public static int computeCRC8DVBS2(byte[] data) {
        int crc = 0x00;
        for (byte b : data) {
            crc ^= b;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = (byte) ((crc << 1) ^ 0xD5);
                } else {
                    crc <<= 1;
                }
            }
        }
        return crc;
    }
}
