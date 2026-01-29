// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p20;

import at.ebutilities.schemata.customerconsent.cmnotification._01p20.ResponseDataType;
import energy.eddie.regionconnector.at.eda.dto.ResponseData;

import java.util.List;

public record ResponseData01p20(ResponseDataType responseData) implements ResponseData {
    @Override
    public String consentId() {
        return responseData.getConsentId();
    }

    @Override
    public String meteringPoint() {
        return responseData.getMeteringPoint();
    }

    @Override
    public List<Integer> responseCodes() {
        return responseData.getResponseCode();
    }
}
