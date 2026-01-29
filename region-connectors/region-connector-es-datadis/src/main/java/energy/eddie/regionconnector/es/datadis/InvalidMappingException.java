// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis;

public class InvalidMappingException extends Exception {

    public InvalidMappingException() {
    }

    public InvalidMappingException(String message) {
        super(message);
    }

}
