// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos.authorizations;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserAuthorizationsResponse(@JsonProperty("response") List<AuthorizedCups> authorizedCups) {
}
