// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class HistoricalDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalDataService.class);
    private final PollingService pollingService;
    private final FrPermissionRequestRepository repository;

    public HistoricalDataService(PollingService pollingService, FrPermissionRequestRepository repository) {
        this.pollingService = pollingService;
        this.repository = repository;
    }

    @Async
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void fetchHistoricalMeterReadings(String permissionId) {
        LOGGER.info("Fetching historical readings for {}", permissionId);
        var permissionRequest = repository.getByPermissionId(permissionId);
        String permissionId1 = permissionRequest.permissionId();
        if (!pollingService.isActiveAndNeedsToBeFetched(permissionRequest)) {
            LOGGER.info("Permission request '{}' is not yet active, skipping data fetch", permissionId1);
            return;
        }

        pollingService.pollTimeSeriesData(permissionRequest);
    }
}
