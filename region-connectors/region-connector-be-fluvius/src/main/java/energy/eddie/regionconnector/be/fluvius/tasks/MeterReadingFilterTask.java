// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component
public class MeterReadingFilterTask implements Predicate<IdentifiableMeteringData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingFilterTask.class);
    private final DataNeedsService dataNeedsService;

    public MeterReadingFilterTask(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {this.dataNeedsService = dataNeedsService;}

    @Override
    public boolean test(IdentifiableMeteringData meteringData) {
        var pr = meteringData.permissionRequest();
        var permissionId = pr.permissionId();
        var dataNeed = (ValidatedHistoricalDataDataNeed) dataNeedsService.getById(pr.dataNeedId());
        var data = meteringData.payload().data().headpoint();
        LOGGER.debug("Filtering data to only contain {} readings for permission request {}",
                     dataNeed.energyType(),
                     permissionId);
        return data != null && data.isEnergyType(dataNeed.energyType());
    }
}
