// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.master.data;

public class CdsServerBuilder {
    private String baseUri;
    private String clientId;
    private String clientSecret;
    private Long id = null;

    public CdsServerBuilder setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    public CdsServerBuilder setAdminClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public CdsServerBuilder setAdminClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public CdsServerBuilder setId(Long id) {
        this.id = id;
        return this;
    }

    public CdsServer build() {
        return new CdsServer(
                id,
                baseUri,
                clientId,
                clientSecret
        );
    }
}