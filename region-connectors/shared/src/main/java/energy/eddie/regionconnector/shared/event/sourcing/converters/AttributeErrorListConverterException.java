// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing.converters;

public class AttributeErrorListConverterException extends RuntimeException {
    public AttributeErrorListConverterException(Throwable cause) {
        super(cause);
    }
}
