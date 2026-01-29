// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt;

import java.security.SecureRandom;

public class SecretGenerator {
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int LENGTH = 10;

    private SecretGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static String generate() {
        StringBuilder builder = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(BASE62.length());
            builder.append(BASE62.charAt(index));
        }
        return builder.toString();
    }
}
