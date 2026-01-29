// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.web;

import energy.eddie.api.agnostic.EddieApiError;

import java.util.List;

public interface ErrorMapper {
    List<EddieApiError> asErrorsList();
}
