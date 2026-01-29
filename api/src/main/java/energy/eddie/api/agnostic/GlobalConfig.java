// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic;

public final class GlobalConfig {
    public static final String ERRORS_PROPERTY_NAME = "errors";
    public static final String ERRORS_JSON_PATH = "$." + ERRORS_PROPERTY_NAME;

    private GlobalConfig() {}
}
