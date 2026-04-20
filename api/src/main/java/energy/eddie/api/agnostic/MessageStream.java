// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for marking methods to indicate that they process or interact
 * with a specific type of message stream. This type is defined by the `value` attribute
 * which specifies the class representing the type of messages handled by the annotated method.
 * <p>
 * This annotation is used to wire message producers, who are implemented as Spring Beans by a {@link RegionConnector},
 * and the consumers, who are implemented by a {@link energy.eddie.api.agnostic.outbound.OutboundConnector}.
 * <br/>
 * <strong>The annotated method must be implemented by a class that is available as a Spring Bean.</strong>
 * <br/>
 * This allows EDDIE to dynamically discover and connect message producers and consumers.
 * The wiring process essentially passes a producer of a stream to all consumers of the same type.
 * <p>
 * The annotation should only be applied to methods with the following signature, where the message type is the same as the class passed to the {@link MessageStream#value} attribute.
 * <pre>
 * {@code
 * void accept(Flux<MessageType> messages){ ... }
 * Flux<MessageType> get(){ ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MessageStream {
    /**
     * Specifies the class representing the type of messages associated with the annotated method.
     *
     * @return the class type associated with the annotated method
     */
    Class<?> value();
}
