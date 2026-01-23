// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SupplyApi {
    Mono<List<Supply>> getSupplies(String authorizedNif, @Nullable String distributorCode);
}