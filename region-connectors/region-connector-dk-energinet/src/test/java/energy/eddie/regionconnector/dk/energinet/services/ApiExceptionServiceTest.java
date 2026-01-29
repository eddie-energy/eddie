// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.exceptions.ApiResponseException;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static energy.eddie.api.v0.PermissionProcessStatus.REVOKED;
import static energy.eddie.api.v0.PermissionProcessStatus.UNFULFILLABLE;
import static energy.eddie.regionconnector.dk.energinet.filter.EnerginetResponseStatusCodes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiExceptionServiceTest {
    private static final WebClientResponseException tooManyRequests = WebClientResponseException.create(
            429,
            "Too Many Requests",
            null,
            null,
            null,
            null
    );
    private static final WebClientResponseException serviceUnavailable = WebClientResponseException.create(
            503,
            "Service Unavailable",
            null,
            null,
            null,
            null
    );
    private static final WebClientResponseException badRequest = new WebClientResponseException(
            400,
            "Bad Request",
            null,
            "#30001: Period not allowed, ToDate is before FromDate.".getBytes(StandardCharsets.UTF_8),
            null
    );
    @Mock
    private Outbox outbox;

    private static Stream<Arguments> provideExceptionAndExpectedStatus() {
        return Stream.of(
                Arguments.of(tooManyRequests, null),
                Arguments.of(serviceUnavailable, null),
                Arguments.of(badRequest, UNFULFILLABLE),
                Arguments.of(new RuntimeException("Some other exception"), null),
                Arguments.of(new ApiResponseException(REQUESTED_AGGREGATION_UNAVAILABLE, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(REQUESTED_AGGREGATION_UNAVAILABLE, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(NO_VALID_METERING_POINTS_IN_LIST, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(METERING_POINT_ID_NOT_18_CHARS_LONG, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(METERING_POINT_NOT_FOUND, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(METERING_POINT_IS_CHILD, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(WRONG_METERING_POINT_ID_OR_WEB_ACCESS_CODE, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(WRONG_TOKEN_TYPE, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(WRONG_NUMBER_OF_ARGUMENTS, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(FROM_DATE_IS_GREATER_THAN_TODAY, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(FROM_DATE_IS_GREATER_THAN_TO_DATE, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(TO_DATE_CAN_NOT_BE_EQUAL_TO_FROM_DATE, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(TO_DATE_IS_GREATER_THAN_TODAY, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(THIRD_PARTY_NOT_FOUND, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(TOKEN_NOT_VALID, ""), UNFULFILLABLE),
                Arguments.of(new ApiResponseException(UNAUTHORIZED, ""), REVOKED),
                Arguments.of(new ApiResponseException(NO_CPR_CONSENT, ""), REVOKED),
                Arguments.of(new ApiResponseException(ERROR_CREATING_TOKEN, ""), REVOKED),
                Arguments.of(new ApiResponseException(TOKEN_ALREADY_DEACTIVATED, ""), REVOKED),
                Arguments.of(new ApiResponseException(NO_ERROR, ""), null),
                Arguments.of(new ApiResponseException(METERING_POINT_RELATION_ALREADY_EXIST, ""), null),
                Arguments.of(new ApiResponseException(METERING_POINT_ALIAS_TOO_LONG, ""), null),
                Arguments.of(new ApiResponseException(WEB_ACCESS_CODE_NOT_8_CHARS_LONG, ""), null),
                Arguments.of(new ApiResponseException(WEB_ACCESS_CODE_CONTAINS_ILLEGAL_CHARS, ""), null),
                Arguments.of(new ApiResponseException(RELATION_NOT_FOUND, ""), null),
                Arguments.of(new ApiResponseException(UNKNOWN_ERROR, ""), null),
                Arguments.of(new ApiResponseException(INTERNAL_SERVER_ERROR, ""), null),
                Arguments.of(new ApiResponseException(TOKEN_REGISTRATION_FAILED, ""), null),
                Arguments.of(new ApiResponseException(TOKEN_ALREADY_ACTIVE, ""), null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideExceptionAndExpectedStatus")
    void handleException(Exception exception, @Nullable PermissionProcessStatus expectedStatus) {
        ApiExceptionService apiExceptionService = new ApiExceptionService(outbox);
        apiExceptionService.handleError("permissionId", exception);

        if (expectedStatus == null) {
            verifyNoInteractions(outbox);
            return;
        }
        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox, times(1)).commit(eventCaptor.capture());
        assertEquals(expectedStatus, eventCaptor.getValue().status());
    }
}
