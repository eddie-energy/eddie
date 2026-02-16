// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.outbound;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a class as an outbound-connector.
 * An outbound-connector defines interfaces that send and receive messages from the eligible party.
 * Outbound-connectors are defined in their own module and started in their own Spring context.
 * Must be used in combination with {@code SpringBootApplication}.
 *
 * @see RawDataOutboundConnector
 * @see ConnectionStatusMessageOutboundConnector
 * @see energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector
 * @see energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector
 * @see energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector
 * @see energy.eddie.api.v0_82.outbound.TerminationConnector
 * @see energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface OutboundConnector {
    String name();
}
