// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.client.creation.responses;

public sealed interface ApiClientCreationResponse permits CreatedCdsClientResponse, NotACdsServerResponse, UnableToRegisterClientResponse, UnsupportedFeatureResponse {
}
