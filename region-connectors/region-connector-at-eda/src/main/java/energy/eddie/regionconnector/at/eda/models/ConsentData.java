// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.models;

import energy.eddie.regionconnector.at.eda.dto.ResponseData;

import java.util.List;
import java.util.Optional;

public record ConsentData(
        List<Integer> responseCodes,
        String message,
        Optional<String> cmConsentId,
        Optional<String> meteringPoint) {

    public static ConsentData fromResponseData(ResponseData responseData) {
        return new ConsentData(
                responseData.responseCodes(),
                statusCodesToMessage(responseData.responseCodes()),
                Optional.ofNullable(responseData.consentId()),
                Optional.ofNullable(responseData.meteringPoint())
        );
    }


    private static String statusCodesToMessage(List<Integer> statusCodes) {
        return statusCodes.stream()
                          .map(ResponseCode::new)
                          .map(ResponseCode::toString)
                          .reduce((a, b) -> a + ", " + b)
                          .orElse("No response codes provided.");
    }
}
