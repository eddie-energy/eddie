// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.persistence;

import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReadingPk;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, MeterReadingPk> {
    @Modifying
    @Query("update MeterReading mr set mr.historicalCollectionStatus = :historicalCollectionStatus where mr.permissionId = :permissionId AND mr.meterUid = :meterUid")
    @Transactional(Transactional.TxType.REQUIRED)
    void updateHistoricalCollectionStatusForMeter(
            @Param("historicalCollectionStatus") PollingStatus historicalCollectionStatus,
            @Param("permissionId") String permissionId,
            @Param("meterUid") String meterUid
    );

    List<MeterReading> findAllByPermissionId(String permissionId);
}
