// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.services.oauth.code.AuthorizationCodeResult;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleAuthorizationServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private CdsServerClientFactory factory;
    @Mock
    private CdsServerClient client;
    @InjectMocks
    private SimpleAuthorizationService authorizationService;
    @Captor
    private ArgumentCaptor<SentToPaEvent> sentToPaEvent;

    @Test
    void testCreateOAuthRequest_returnsCorrectUriAndEmitsSentToPaEvent() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setId(1L)
                .build();
        var redirectUri = URI.create("http://localhost");
        when(factory.get(cdsServer)).thenReturn(client);
        when(client.createAuthorizationUri(List.of(Scopes.CUSTOMER_DATA_SCOPE)))
                .thenReturn(Optional.of(new AuthorizationCodeResult(redirectUri, "state")));

        // When
        var res = authorizationService.createOAuthRequest(cdsServer, "pid");

        // Then
        assertEquals(redirectUri, res);
        verify(outbox).commit(sentToPaEvent.capture());
        var event = sentToPaEvent.getValue();
        assertFalse(event.isPushedAuthorizationRequest());
        assertEquals(redirectUri.toString(), event.redirectUri());
    }

    @Test
    void testCreateOAuthRequest_forInvalidRequest_returnsNull() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setId(1L)
                .build();
        when(factory.get(cdsServer)).thenReturn(client);
        when(client.createAuthorizationUri(List.of(Scopes.CUSTOMER_DATA_SCOPE))).thenReturn(Optional.empty());

        // When
        var res = authorizationService.createOAuthRequest(cdsServer, "pid");

        // Then
        assertNull(res);
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.MALFORMED, event.status())
        )));
    }
}