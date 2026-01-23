// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.regionconnector.dk.energinet.exceptions.ApiResponseException;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkRevokedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkUnfulfillableEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

import static energy.eddie.regionconnector.dk.energinet.filter.EnerginetResponseStatusCodes.*;

/**
 * Service for handling exceptions thrown during API calls to Energinet
 */
@Service
public class ApiExceptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionService.class);

    private final Outbox outbox;

    public ApiExceptionService(Outbox outbox) {this.outbox = outbox;}


    public void handleError(String permissionId, Throwable throwable) {
        if (throwable instanceof ApiResponseException apiResponseException) {
            handleApiResponseException(apiResponseException, permissionId);
            return;
        }

        if (throwable instanceof WebClientResponseException.Unauthorized unauthorized) {
            revoked(unauthorized, permissionId);
            return;
        }

        if (throwable instanceof WebClientResponseException webClientResponseException) {
            // BadRequest might return something like this "#20013: No meteringpoints in request conforms to valid meteringpoint format."
            var body = webClientResponseException.getResponseBodyAsString();
            var errorCode = errorCodeFromBody(body);

            if (errorCode.isPresent()) {
                handleApiResponseException(new ApiResponseException(errorCode.get(), body), permissionId);
                return;
            }
        }

        LOGGER.atError()
              .addArgument(permissionId)
              .setCause(throwable)
              .log("Something went wrong while fetching data for permission request {} from Energinet");
    }

    private void handleApiResponseException(ApiResponseException apiResponseException, String permissionId) {
        switch (apiResponseException.errorCode()) {
            case REQUESTED_AGGREGATION_UNAVAILABLE -> unfulfillable(permissionId,
                                                                    apiResponseException,
                                                                    "Requested aggregation for permission request {} is not available: {}");
            case NO_VALID_METERING_POINTS_IN_LIST,
                 METERING_POINT_ID_NOT_18_CHARS_LONG,
                 METERING_POINT_NOT_FOUND,
                 METERING_POINT_IS_CHILD,
                 METERING_POINT_BLOCKED,
                 WRONG_METERING_POINT_ID_OR_WEB_ACCESS_CODE -> unfulfillable(permissionId,
                                                                             apiResponseException,
                                                                             "Provided metering point for permission request {} seems to be invalid: {}");
            case WRONG_TOKEN_TYPE,
                 TOKEN_NOT_VALID,
                 WRONG_NUMBER_OF_ARGUMENTS,
                 FROM_DATE_IS_GREATER_THAN_TODAY,
                 FROM_DATE_IS_GREATER_THAN_TO_DATE,
                 TO_DATE_CAN_NOT_BE_EQUAL_TO_FROM_DATE,
                 TO_DATE_IS_GREATER_THAN_TODAY,
                 INVALID_DATE_FORMAT -> unfulfillable(permissionId,
                                                      apiResponseException,
                                                      "The performed request for permission request {} was invalid: {}");
            case THIRD_PARTY_NOT_FOUND -> {
                LOGGER.error("Third party not found on Energinet, check if the configuration is correct");
                unfulfillable(permissionId, apiResponseException);
            }
            case UNAUTHORIZED, NO_CPR_CONSENT, ERROR_CREATING_TOKEN, TOKEN_ALREADY_DEACTIVATED ->
                    revoked(apiResponseException, permissionId);
            default -> LOGGER.atError()
                             .addArgument(permissionId)
                             .addArgument(apiResponseException::errorCode)
                             .addArgument(apiResponseException::errorText)
                             .setCause(apiResponseException)
                             .log("An error occurred while fetching data for permission request {} from Energinet. Error code: {}. Error text: {}");
        }
    }

    private void revoked(Exception exception, String permissionId) {
        LOGGER.info("Received unauthorized response for permission request {}. Permission has been revoked.",
                    permissionId);
        outbox.commit(new DkRevokedEvent(permissionId, exception.getMessage()));
    }

    private Optional<Integer> errorCodeFromBody(String body) {
        try {
            // Example body "#30001: Period not allowed, ToDate is before FromDate."
            // Extract the error code which is between '#' and ':'
            String errorCodeStr = body.substring(1, body.indexOf(":")).trim();
            return Optional.of(Integer.parseInt(errorCodeStr));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    private void unfulfillable(String permissionId, ApiResponseException apiResponseException, String s) {
        LOGGER.atWarn()
              .addArgument(permissionId)
              .addArgument(apiResponseException::errorText)
              .log(s);
        unfulfillable(permissionId, apiResponseException);
    }

    private void unfulfillable(String permissionId, ApiResponseException apiResponseException) {
        outbox.commit(new DkUnfulfillableEvent(permissionId, apiResponseException.errorText()));
    }
}
