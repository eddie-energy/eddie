// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.revocation.RevocationResult;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {
    @Mock
    private CdsServerRepository serverRepository;
    @Mock
    private CdsServerClientFactory factory;
    @Mock
    private CdsServerClient client;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private TerminationService terminationService;

    static Stream<Arguments> testTerminate_withFailureDuringTermination_emitsFailedToTerminate() {
        return Stream.of(
                Arguments.of(new RevocationResult.ServiceUnavailable()),
                Arguments.of(new RevocationResult.InvalidRevocationRequest("error_code"))
        );
    }

    @Test
    void testTerminate_sendsTermination() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost")
                .build();
        when(serverRepository.getReferenceById(1L)).thenReturn(cdsServer);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setCdsServer(1L)
                .build();
        when(factory.get(pr)).thenReturn(client);
        when(client.revokeToken(pr)).thenReturn(Mono.just(new RevocationResult.SuccessfulRevocation()));


        // When
        terminationService.terminate(pr);

        // Then
        verify(outbox).commit(assertArg(e -> assertAll(
                                                () -> assertEquals("pid", e.permissionId()),
                                                () -> assertEquals(PermissionProcessStatus.EXTERNALLY_TERMINATED, e.status())
                                        )
        ));
    }

    @ParameterizedTest
    @MethodSource
    void testTerminate_withFailureDuringTermination_emitsFailedToTerminate(RevocationResult revocationResult) {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost")
                .build();
        when(serverRepository.getReferenceById(1L)).thenReturn(cdsServer);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setCdsServer(1L)
                .build();
        when(factory.get(pr)).thenReturn(client);
        when(client.revokeToken(pr)).thenReturn(Mono.just(revocationResult));


        // When
        terminationService.terminate(pr);

        // Then
        verify(outbox).commit(assertArg(e -> assertAll(
                                                () -> assertEquals("pid", e.permissionId()),
                                                () -> assertEquals(PermissionProcessStatus.FAILED_TO_TERMINATE, e.status())
                                        )
        ));
    }

    @Test
    void testTerminate_whereServerDoesNotAllowTermination_emitsExternallyTerminated() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setBaseUri("http://localhost")
                .build();
        when(serverRepository.getReferenceById(1L)).thenReturn(cdsServer);
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setCdsServer(1L)
                .build();
        when(factory.get(pr)).thenReturn(client);
        when(client.revokeToken(pr))
                .thenReturn(Mono.just(new RevocationResult.InvalidRevocationRequest(RevocationResult.InvalidRevocationRequest.UNSUPPORTED_TOKEN_TYPE)));


        // When
        terminationService.terminate(pr);

        // Then
        verify(outbox).commit(assertArg(e -> assertAll(
                                                () -> assertEquals("pid", e.permissionId()),
                                                () -> assertEquals(PermissionProcessStatus.EXTERNALLY_TERMINATED, e.status())
                                        )
        ));
    }
}