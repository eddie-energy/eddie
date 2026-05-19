// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12;

public enum DocumentType {
    REQUEST_PERMISSION_MARKET_DOCUMENT("request-permission-market-document");

    private final String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
