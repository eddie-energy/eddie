// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors;

import java.util.function.Supplier;

public class SecretLoadingException extends Exception implements Supplier<SecretLoadingException> {
    private static final String EXCEPTION_MESSAGE = "Failed to load secret";

    public SecretLoadingException() {
        super(EXCEPTION_MESSAGE);
    }

    public SecretLoadingException(String alias) {
        super(EXCEPTION_MESSAGE + " for " + alias);
    }

    public SecretLoadingException(String alias, Throwable cause) {
        super(EXCEPTION_MESSAGE + " for " + alias, cause);
    }

    @Override
    public SecretLoadingException get() {
        return this;
    }
}
