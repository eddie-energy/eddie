// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

public class UnsupportedObisCodeException extends Exception {
    public UnsupportedObisCodeException(String obisCode) {
        super(obisCode);
    }
}
