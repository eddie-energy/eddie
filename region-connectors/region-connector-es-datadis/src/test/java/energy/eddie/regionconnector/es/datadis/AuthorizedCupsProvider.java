// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis;

import energy.eddie.regionconnector.es.datadis.dtos.authorizations.AuthorizedCups;
import energy.eddie.regionconnector.es.datadis.dtos.authorizations.UserAuthorizationsResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AuthorizedCupsProvider {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static AuthorizedCups loadAuthorizedCups() throws IOException {
        try (InputStream is = AuthorizedCupsProvider.class.getClassLoader()
                                                          .getResourceAsStream("authorizedCups.json")) {
            return OBJECT_MAPPER.readValue(is, AuthorizedCups.class);
        }
    }

    public static UserAuthorizationsResponse loadUserAuthorizationResponse() throws IOException {
        try (InputStream is = AuthorizedCupsProvider.class.getClassLoader()
                                                          .getResourceAsStream("authorizedCups.json")) {
            return new UserAuthorizationsResponse(List.of(OBJECT_MAPPER.readValue(is, AuthorizedCups.class)));
        }
    }
}
