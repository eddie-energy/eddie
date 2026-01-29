// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.persistence;

import energy.eddie.api.agnostic.process.model.persistence.FullPermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@org.springframework.stereotype.Repository
public interface BePermissionRequestRepository extends Repository<FluviusPermissionRequest, String>, FullPermissionRequestRepository<FluviusPermissionRequest> {

    @Override
    @Query(
            value = "SELECT permission_id, connection_id, data_need_id, status, data_start, data_end, granularity, flow, created, short_url_identifier " +
                    "FROM be_fluvius.permission_request WHERE status = 'SENT_TO_PERMISSION_ADMINISTRATOR' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    List<FluviusPermissionRequest> findStalePermissionRequests(@Param("hours") int stalenessDuration);
}
