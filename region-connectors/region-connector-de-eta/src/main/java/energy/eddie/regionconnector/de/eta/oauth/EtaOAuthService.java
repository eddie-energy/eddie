package energy.eddie.regionconnector.de.eta.oauth;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;

import com.nimbusds.oauth2.sdk.ParseException;

@Service
public class EtaOAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaOAuthService.class);
    private static final String SUCCESS_KEY = "success";

    private final DeEtaPlusConfiguration configuration;

    public EtaOAuthService(DeEtaPlusConfiguration configuration) {
        this.configuration = configuration;
    }

    public Mono<OAuthTokenResponse> exchangeCodeForToken(String code, String openid) {
        return Mono.fromCallable(() -> performTokenExchange(code, openid))
                   .subscribeOn(Schedulers.boundedElastic())
                   .onErrorResume(
                           error -> {
                               LOGGER.error("Error during token exchange", error);
                               return Mono.just(new OAuthTokenResponse(null, false));
                           });
    }

    private OAuthTokenResponse performTokenExchange(String code, String openid) throws IOException, ParseException {
        LOGGER.info("Exchanging authorization token for access token");

        URI tokenEndpoint = UriComponentsBuilder
                .fromUriString(configuration.oauth().tokenUrl())
                .queryParam("token", code)
                .queryParam("openid", openid)
                .queryParam("client_id", configuration.oauth().clientId())
                .build()
                .toUri();

        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.PUT, tokenEndpoint);
        request.setAccept("application/json");

        HTTPResponse response = request.send();

        if (!response.indicatesSuccess()) {
            LOGGER.warn("Token exchange returned unsuccessful response: {}", response.getBody());
            return new OAuthTokenResponse(null, false);
        }

        var jsonObject = response.getBodyAsJSONObject();

        boolean success = false;
        if (jsonObject.containsKey(SUCCESS_KEY) && jsonObject.get(SUCCESS_KEY) instanceof Boolean successFlag) {
            success = successFlag;
        }

        if (!success) {
            LOGGER.warn("Token exchange returned unsuccessful response: success flag is false");
            return new OAuthTokenResponse(null, false);
        }

        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) jsonObject.get("data");

        String token = null;
        String refreshTokenString = null;

        if (data != null) {
            token = (String) data.get("token");
            refreshTokenString = (String) data.get("refreshToken");
        }

        if (token == null) {
            LOGGER.warn("Token exchange returned a response without a token");
            return new OAuthTokenResponse(null, false);
        }
        LOGGER.info("Successfully exchanged token for access token");
        return new OAuthTokenResponse(new OAuthTokenResponse.TokenData(token, refreshTokenString), success);
    }
}
