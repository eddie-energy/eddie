// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import java.time.LocalDate;

public class SimpleEdaCMRevoke implements EdaCMRevoke {

    private String meteringPoint;
    private String consentId;
    private LocalDate consentEnd;

    @Override
    public String meteringPoint() {
        return meteringPoint;
    }

    @Override
    public String consentId() {
        return consentId;
    }

    @Override
    public LocalDate consentEnd() {
        return consentEnd;
    }

    public SimpleEdaCMRevoke setMeteringPoint(String meteringPoint) {
        this.meteringPoint = meteringPoint;
        return this;
    }

    public SimpleEdaCMRevoke setConsentId(String consentId) {
        this.consentId = consentId;
        return this;
    }

    public SimpleEdaCMRevoke setConsentEnd(LocalDate consentEnd) {
        this.consentEnd = consentEnd;
        return this;
    }
}
