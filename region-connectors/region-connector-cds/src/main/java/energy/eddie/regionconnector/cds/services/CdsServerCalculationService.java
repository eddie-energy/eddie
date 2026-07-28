// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotSupportedResult;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Set;

@Service
public class CdsServerCalculationService {
    private final DataNeedCalculationService calculationService;
    private final CdsServerClientFactory factory;


    public CdsServerCalculationService(DataNeedCalculationService calculationService, CdsServerClientFactory factory) {
        this.calculationService = calculationService;
        this.factory = factory;
    }

    public DataNeedCalculationResult calculate(
            String dataNeedId,
            CdsServer cdsServer,
            ZonedDateTime referenceDateTime
    ) {
        var calc = calculationService.calculate(dataNeedId, referenceDateTime);
        if (!(calc instanceof ValidatedHistoricalDataDataNeedResult vhdCalc)) {
            return calc;
        }
        var energyTypes = factory.get(cdsServer)
                                 .masterData()
                                 .blockOptional()
                                 .map(CdsServerMasterData::energyTypes)
                                 .orElse(Set.of());
        var energyType = vhdCalc.dataNeed().energyType();
        if (energyTypes.contains(energyType)) {
            return calc;
        }
        return new DataNeedNotSupportedResult("CDS Server with ID %s does not support the energy type %s"
                                                      .formatted(cdsServer.id(), energyType));
    }
}
