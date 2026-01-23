// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.retransmission;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonRetransmissionServiceTest {
    @Mock
    private PermissionRequestRepository<PermissionRequest> repository;
    @Mock
    private PollingFunction<PermissionRequest> pollingFunction;
    @Mock
    private RetransmissionValidation validation;
    @InjectMocks
    private CommonRetransmissionService<PermissionRequest> service;

    @Test
    void testRequestRetransmission_ifValidationFails_emitsError() {
        // Given
        Optional<PermissionRequest> pr = Optional.of(new SimplePermissionRequest("pid",
                                                                                 "cid",
                                                                                 "dnid",
                                                                                 PermissionProcessStatus.ACCEPTED));
        when(repository.findByPermissionId("pid")).thenReturn(pr);
        var today = LocalDate.now(ZoneOffset.UTC);
        var request = new RetransmissionRequest("rc-id", "pid", today, today);
        var failure = new Failure("pid", ZonedDateTime.now(ZoneOffset.UTC), "whatever");
        when(validation.validate(pr, request)).thenReturn(failure);

        // When
        var res = service.requestRetransmission(request);

        // Then
        StepVerifier.create(res)
                    .expectNext(failure)
                    .verifyComplete();
    }

    @Test
    void testRequestRetransmission_ifValidationSuccessful_emitsPollingResult() {
        // Given
        var permissionRequest = new SimplePermissionRequest("pid",
                                                            "cid",
                                                            "dnid",
                                                            PermissionProcessStatus.ACCEPTED);
        Optional<PermissionRequest> pr = Optional.of(permissionRequest);
        when(repository.findByPermissionId("pid")).thenReturn(pr);
        var today = LocalDate.now(ZoneOffset.UTC);
        var request = new RetransmissionRequest("rc-id", "pid", today, today);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        when(validation.validate(pr, request)).thenReturn(new Success("pid", now));
        var dataNotAvailable = new DataNotAvailable("pid", now);
        when(pollingFunction.poll(permissionRequest, request)).thenReturn(Mono.just(dataNotAvailable));

        // When
        var res = service.requestRetransmission(request);

        // Then
        StepVerifier.create(res)
                    .expectNext(dataNotAvailable)
                    .verifyComplete();
    }
}