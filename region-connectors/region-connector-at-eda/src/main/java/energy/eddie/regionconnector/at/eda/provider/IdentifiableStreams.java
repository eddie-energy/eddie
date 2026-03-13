// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider;

import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.UnaryOperator;

@Component
public class IdentifiableStreams {
    private final Flux<IdentifiableConsumptionRecord> consumptionRecordStream;
    private final Flux<IdentifiableMasterData> masterDataStream;
    private final Flux<IdentifiableECMPList> ecmpListStream;

    public IdentifiableStreams(
            ObjectProvider<UnaryOperator<IdentifiableConsumptionRecord>> duplicateDataFilter,
            EdaAdapter edaAdapter
    ) {
        var filter = duplicateDataFilter.getIfAvailable(UnaryOperator::identity);
        this.consumptionRecordStream = edaAdapter.getConsumptionRecordStream()
                                                 .map(filter)
                                                 .share();
        this.masterDataStream = edaAdapter.getMasterDataStream();
        this.ecmpListStream = edaAdapter.getECMPListStream();
    }

    public Flux<IdentifiableConsumptionRecord> consumptionRecordStream() {
        return consumptionRecordStream;
    }

    public Flux<IdentifiableMasterData> masterDataStream() {
        return masterDataStream;
    }

    public Flux<IdentifiableECMPList> ecmpListStream() {
        return ecmpListStream;
    }
}
