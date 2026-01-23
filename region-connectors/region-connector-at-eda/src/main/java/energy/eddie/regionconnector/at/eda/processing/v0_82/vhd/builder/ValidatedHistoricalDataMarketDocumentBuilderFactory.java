// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

public class ValidatedHistoricalDataMarketDocumentBuilderFactory {
    public ValidatedHistoricalDataMarketDocumentBuilder create() {
        return new ValidatedHistoricalDataMarketDocumentBuilder(new SeriesPeriodBuilderFactory(),
                                                                new TimeSeriesBuilderFactory());
    }
}
