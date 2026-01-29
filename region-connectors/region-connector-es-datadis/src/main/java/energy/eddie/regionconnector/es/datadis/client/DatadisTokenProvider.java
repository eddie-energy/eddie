// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.client;

import reactor.core.publisher.Mono;

public interface DatadisTokenProvider {
    Mono<String> getToken();
}
