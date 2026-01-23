// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.utils;

public enum ConversionFactor {
    KWH_TO_WH(1000),
    MWH_TO_WH(1000000),
    GWH_TO_WH(1000000000);

    private final double factor;

    ConversionFactor(double factor) {
        this.factor = factor;
    }

    public double getFactor() {
        return factor;
    }
}
