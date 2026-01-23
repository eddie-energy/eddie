// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest;

import energy.eddie.api.agnostic.outbound.EnableSwaggerDoc;
import energy.eddie.api.agnostic.outbound.OutboundConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OutboundConnector(name = "rest")
@EnableSwaggerDoc
@SpringBootApplication
public class RestOutboundConnector {
}
