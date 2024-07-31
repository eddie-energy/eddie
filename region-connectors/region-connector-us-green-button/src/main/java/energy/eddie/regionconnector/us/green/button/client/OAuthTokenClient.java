package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.GreenButtonSpringConfig;
import energy.eddie.regionconnector.us.green.button.api.TokenApi;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.oauth.dto.AccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.dto.ClientAccessTokenResponse;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithCodeRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.AccessTokenWithRefreshTokenRequest;
import energy.eddie.regionconnector.us.green.button.oauth.request.ClientAccessTokenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class OAuthTokenClient implements TokenApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthTokenClient.class);
    private static final String TOKEN_URI = "/oauth/token";
    private final WebClient webClient;

    public OAuthTokenClient(
            String baseUrl,
            String clientId,
            String clientSecret,
            GreenButtonConfiguration greenButtonConfiguration
    ) {
        var authorizationHeader = "Basic " + Base64.getEncoder()
                                                   .encodeToString((clientId + ":" + clientSecret).getBytes(
                                                           StandardCharsets.UTF_8));
        this.webClient = new GreenButtonSpringConfig().webClient(greenButtonConfiguration)
                                                      .mutate()
                                                      .defaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeader)
                                                      .baseUrl(baseUrl)
                                                      .build();
    }

    @Override
    public Mono<AccessTokenResponse> accessToken(AccessTokenWithCodeRequest tokenRequest) {
        var multipartBodyBuilder = new MultipartBodyBuilder();

        return webClient.post()
                        .uri(TOKEN_URI)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(tokenRequest.assembleMultipartBodyBuilder(
                                multipartBodyBuilder).build()))
                        .retrieve()
                        .bodyToMono(AccessTokenResponse.class)
                        .doOnError(e -> LOGGER.error("Failed to get access token", e));
    }

    @Override
    public Mono<AccessTokenResponse> accessToken(AccessTokenWithRefreshTokenRequest tokenRequest) {
        var multipartBodyBuilder = new MultipartBodyBuilder();

        return webClient.post()
                        .uri(TOKEN_URI)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(tokenRequest.assembleMultipartBodyBuilder(
                                multipartBodyBuilder).build()))
                        .retrieve()
                        .bodyToMono(AccessTokenResponse.class);
    }

    @Override
    public Mono<ClientAccessTokenResponse> clientAccessToken(ClientAccessTokenRequest tokenRequest) {
        var multipartBodyBuilder = new MultipartBodyBuilder();

        return webClient.post()
                        .uri(TOKEN_URI)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(tokenRequest.assembleMultipartBodyBuilder(
                                multipartBodyBuilder).build()))
                        .retrieve()
                        .bodyToMono(ClientAccessTokenResponse.class);
    }
}
