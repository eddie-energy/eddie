// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ContractApi {
    Mono<List<ContractDetails>> getContractDetails(String authorizedNif, String distributorCode, String cups);
}
