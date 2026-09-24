// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

/**
 * Describes the concrete type used to deserialize a document.
 * Implementations, such as {@link CimMessageTypes}, provide the concrete class
 * of the supported document types.
 */
public interface MessageType {
    /**
     * The concrete class used to deserialize documents of this type.
     *
     * @return the concrete class of the document type.
     */
    Class<?> messageClass();
}
