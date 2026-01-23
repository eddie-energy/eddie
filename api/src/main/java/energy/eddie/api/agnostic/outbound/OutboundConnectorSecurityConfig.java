// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.outbound;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, that allows Outbound Connectors to define a {@code SecurityFilterChain}.
 * Classes with this Annotation are scanned for security chains during startup of eddie core.
 * Bear in mind, that such classes are loaded before the context of the Outbound Connectors is loaded.
 * Consequently, Beans defined in the Outbound Connector cannot be accessed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface OutboundConnectorSecurityConfig {
}
