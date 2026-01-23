// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {
    @Mock
    private BePermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private RetryService retryService;
    @Captor
    private ArgumentCaptor<ValidatedEvent> validatedCaptor;

    @Test
    void testRetry_sendsValidatedEvent_forPermissionRequests() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND))
                .thenReturn(List.of(
                        DefaultFluviusPermissionRequestBuilder.create()
                                .status(PermissionProcessStatus.UNABLE_TO_SEND)
                                .build()
                ));

        // When
        retryService.retry();

        // Then
        verify(outbox).commit(validatedCaptor.capture());
        var res = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(Flow.B2C, res.flow()),
                () -> assertEquals(Granularity.PT15M, res.granularity()),
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now, res.end())
        );
    }
}