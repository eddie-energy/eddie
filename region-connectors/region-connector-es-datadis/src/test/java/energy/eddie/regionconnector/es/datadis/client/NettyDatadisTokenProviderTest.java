package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NettyDatadisTokenProviderTest {

    @Test
    @Disabled("Integration test, that needs real credentials")
    void getToken_withValidCredentials_returnsToken() {
        NettyDatadisTokenProvider uut = new NettyDatadisTokenProvider(
                new MyDatadisConfig("replace", "replace"),
                HttpClient.create(),
                new DatadisEndpoints());

        StepVerifier.create(uut.getToken())
                .expectNextMatches(token -> token.length() > 0)
                .verifyComplete();
    }

    @Test
    void getToken_withInvalidCredentials_returnsError() {
        NettyDatadisTokenProvider uut = new NettyDatadisTokenProvider(
                new MyDatadisConfig("x", "x"),
                HttpClient.create(), new DatadisEndpoints());

        StepVerifier.create(uut.getToken())
                .expectError(TokenProviderException.class)
                .verify();
    }


    @Test
    void updateTokenAndExpiry_WhenReceivingInvalidToken_returnsError() {
        HttpClient mock = mock(HttpClient.class);

        NettyDatadisTokenProvider uut = new NettyDatadisTokenProvider(
                new MyDatadisConfig("x", "x"),
                mock, new DatadisEndpoints());

        StepVerifier.create(uut.updateTokenAndExpiry("invalid token"))
                .expectError(TokenProviderException.class)
                .verify();
    }

    @Test
    void updateTokenAndExpiry_WhenReceivingToken_updatesExpirationDate() {
        // random token with set exp time
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE2OTMyMDYzODZ9.PVzcUigB84KE6A_Fjlha36T487gaF7ZxktREpDImtIQ";
        long timestamp = 1693206386;
        HttpClient mock = mock(HttpClient.class);

        NettyDatadisTokenProvider uut = new NettyDatadisTokenProvider(
                new MyDatadisConfig("x", "x"),
                mock, new DatadisEndpoints());

        StepVerifier.create(uut.updateTokenAndExpiry(token))
                .expectNext(token)
                .verifyComplete();

        assertEquals(timestamp, uut.getExpiryTime());
    }

    @Test
    @SuppressWarnings("java:S5778")
    void getToken_withExpiredToken_triesToFetchNewToken() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE2OTMyMDYzODZ9.PVzcUigB84KE6A_Fjlha36T487gaF7ZxktREpDImtIQ";

        HttpClient mock = mock(HttpClient.class);
        HttpClient.RequestSender requestMock = mock(HttpClient.RequestSender.class);
        when(mock.post()).thenReturn(requestMock);
        when(requestMock.uri(any(URI.class))).thenReturn(requestMock);

        NettyDatadisTokenProvider uut = new NettyDatadisTokenProvider(
                new MyDatadisConfig("x", "x"),
                mock, new DatadisEndpoints());

        uut.updateTokenAndExpiry(token).block();
        assertThrows(NullPointerException.class, () -> uut.getToken().block());
        verify(requestMock, times(1)).sendForm(any());
    }

    @Test
    @SuppressWarnings("java:S5778")
    void getToken_withNoToken_triesToFetchNewToken() {
        HttpClient mock = mock(HttpClient.class);
        HttpClient.RequestSender requestMock = mock(HttpClient.RequestSender.class);
        when(mock.post()).thenReturn(requestMock);
        when(requestMock.uri(any(URI.class))).thenReturn(requestMock);

        NettyDatadisTokenProvider uut = new NettyDatadisTokenProvider(
                new MyDatadisConfig("x", "x"),
                mock, new DatadisEndpoints());

        assertThrows(NullPointerException.class, () -> uut.getToken().block());
        verify(requestMock, times(1)).sendForm(any());

    }

    private static class MyDatadisConfig implements DatadisConfig {
        private final String username;
        private final String password;

        public MyDatadisConfig(String username, String password) {
            this.username = username;
            this.password = password;
        }


        @Override
        public String username() {
            return username;
        }

        @Override
        public String password() {
            return password;
        }
    }
}