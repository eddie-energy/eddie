// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.oauth.request;

import energy.eddie.regionconnector.us.green.button.oauth.enums.OAuthGrantType;
import org.springframework.http.client.MultipartBodyBuilder;

public class ClientAccessTokenRequest extends OAuthTokenRequest {
    public ClientAccessTokenRequest() {
        super(OAuthGrantType.CLIENT_CREDENTIALS);
    }

    @Override
    public MultipartBodyBuilder assembleMultipartBodyBuilder(MultipartBodyBuilder multipartBodyBuilder) {
        multipartBodyBuilder.part("grant_type", getGrantType().getGrantType());

        return multipartBodyBuilder;
    }
}
