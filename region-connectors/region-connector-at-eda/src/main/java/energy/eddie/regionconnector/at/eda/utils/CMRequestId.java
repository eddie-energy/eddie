// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.utils;

import jakarta.annotation.Nullable;
import org.apache.commons.codec.binary.Base32;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

public class CMRequestId {
    private final String messageId;
    @Nullable
    private String base32;

    /**
     * This class represents a CMRequestId as defined in the schema documentation found on:
     * <a href="https://www.ebutilities.at/schemas/149">ebutilities</a> in the documents section.
     * <p>
     * The CMRequestId is created via the following process:
     *  <ol>
     *      <li> compute CRC32 fom the messageId </li>
     *      <li> compute CRC8 from the CRC32 </li>
     *      <li> checksum = append CRC8 to CRC32 </li>
     *      <li> CMRequestId = base32 encoding of the checksum </li>
     *  </ol>
     *
     * @param messageId the message id to be used as input for the request id
     */
    public CMRequestId(String messageId) {
        requireNonNull(messageId);

        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return getBase32();
    }

    private String getBase32() {
        if (base32 == null) {
            var crc32 = (int) CRC.computeCRC32(requireNonNull(messageId).getBytes(StandardCharsets.UTF_8));
            byte[] crc32Bytes = ByteBuffer.allocate(Integer.BYTES).putInt(crc32).array();
            var crc8 = CRC.computeCRC8DVBS2(crc32Bytes);
            // append crc8 to crc32
            byte[] concatenated = new byte[crc32Bytes.length + 1];
            System.arraycopy(crc32Bytes, 0, concatenated, 0, crc32Bytes.length);
            concatenated[concatenated.length - 1] = (byte) crc8;

            Base32 base32Obj = new Base32();
            this.base32 = base32Obj.encodeAsString(concatenated);
        }

        return base32;
    }
}
