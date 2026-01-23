// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private AtPermissionRequestRepository repository;
    @InjectMocks
    private RetryService retryService;
    @Captor
    private ArgumentCaptor<SimpleEvent> eventCaptor;

    public static Stream<Arguments> testRetry_emitsPermissionRequest_withFollowUpState() {
        return Stream.of(
                Arguments.of(PermissionProcessStatus.UNABLE_TO_SEND, PermissionProcessStatus.VALIDATED),
                Arguments.of(PermissionProcessStatus.FAILED_TO_TERMINATE,
                             PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testRetry_emitsPermissionRequest_withFollowUpState(
            PermissionProcessStatus current,
            PermissionProcessStatus next
    ) {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", "mid", "dsoId", start, end,
                                                         AllowedGranularity.PT15M,
                                                         current, "",
                                                         "consentId", ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.findByStatusIn(any()))
                .thenReturn(List.of(permissionRequest));

        // When
        retryService.retry();

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertEquals(next, res.status());
    }

    @Test
    void testRetry_emitsNothing_withInvalidState() {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", "mid", "dsoId", start, end,
                                                         AllowedGranularity.PT15M,
                                                         PermissionProcessStatus.CREATED, "",
                                                         "consentId", ZonedDateTime.now(ZoneOffset.UTC));
        when(repository.findByStatusIn(any()))
                .thenReturn(List.of(permissionRequest));

        // When
        retryService.retry();

        // Then
        verify(outbox, never()).commit(any());
    }
}