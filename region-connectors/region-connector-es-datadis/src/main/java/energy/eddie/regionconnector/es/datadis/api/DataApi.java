// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DataApi {

    Mono<List<MeteringData>> getConsumptionKwh(MeteringDataRequest meteringDataRequest);
}