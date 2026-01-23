// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos.exceptions;

public class NoSupplyForMeteringPointException extends Exception {

    public NoSupplyForMeteringPointException(String message) {
        super(message);
    }
}
