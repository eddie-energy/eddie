// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.cim.v0_82.pmd.IntermediatePermissionMarketDocumentV0_82;
import energy.eddie.regionconnector.shared.cim.v1_12.rpmd.IntermediateRequestPermissionMarketDocument;
import reactor.core.publisher.Flux;

import java.time.ZoneId;

public class IntermediatePermissionMarketDocumentFactory<T extends PermissionRequest> {
    protected final String customerIdentifier;
    protected final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    protected final String countryCode;
    protected final ZoneId zoneId;

    public IntermediatePermissionMarketDocumentFactory(
            String customerIdentifier,
            TransmissionScheduleProvider<T> transmissionScheduleProvider,
            String countryCode,
            ZoneId zoneId
    ) {
        this.customerIdentifier = customerIdentifier;
        this.transmissionScheduleProvider = transmissionScheduleProvider;
        this.countryCode = countryCode;
        this.zoneId = zoneId;
    }

    public IntermediatePermissionMarketDocument create(
            T permissionRequest,
            PermissionProcessStatus status,
            DataNeed dataNeed,
            CommonInformationModelVersions version
    ) {
        return switch (version) {
            case CommonInformationModelVersions.V0_82 -> new IntermediatePermissionMarketDocumentV0_82<>(
                    permissionRequest,
                    status,
                    customerIdentifier,
                    transmissionScheduleProvider,
                    countryCode,
                    zoneId,
                    dataNeed);
            case CommonInformationModelVersions.V1_12 ->
                    new IntermediateRequestPermissionMarketDocument<>(permissionRequest,
                                                                      customerIdentifier,
                                                                      transmissionScheduleProvider,
                                                                      countryCode,
                                                                      zoneId,
                                                                      dataNeed,
                                                                      status);
            default -> throw new UnsupportedOperationException("Unsupported CIM version: " + version);
        };
    }

    public Flux<IntermediatePermissionMarketDocument> each(
            T permissionRequest,
            PermissionProcessStatus status,
            DataNeed dataNeed
    ) {
        return Flux.just(
                create(permissionRequest, status, dataNeed, CommonInformationModelVersions.V0_82),
                create(permissionRequest, status, dataNeed, CommonInformationModelVersions.V1_12)
        );
    }
}
