// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder;

public record SeriesPeriodBuilderFactory() {

    public SeriesPeriodBuilder create() {
        return new SeriesPeriodBuilder();
    }
}