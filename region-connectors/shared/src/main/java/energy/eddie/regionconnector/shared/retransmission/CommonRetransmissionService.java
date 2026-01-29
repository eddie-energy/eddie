// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.retransmission;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import reactor.core.publisher.Mono;

/**
 * A service that is used to request validated historical data for retransmission requests
 *
 * @param <T> The type of permission request
 */
public class CommonRetransmissionService<T extends PermissionRequest> implements RegionConnectorRetransmissionService {
    private final PermissionRequestRepository<T> repository;
    private final PollingFunction<T> pollingFunction;
    private final RetransmissionValidation validation;

    /**
     * Creates the {@link CommonRetransmissionService}.
     *
     * @param repository      Is used to request the permission request associated with the retransmission request
     * @param pollingFunction Used to poll the validated historical data
     * @param validation      Validates the retransmission request
     */
    public CommonRetransmissionService(
            PermissionRequestRepository<T> repository,
            PollingFunction<T> pollingFunction,
            RetransmissionValidation validation
    ) {
        this.repository = repository;
        this.validation = validation;
        this.pollingFunction = pollingFunction;
    }

    /**
     * Validates the retransmission request and the requests the validated historical data if the validation was successful.
     *
     * @param retransmissionRequest the request specifying the data to be retransmitted
     * @return the result of the retransmission request
     */
    @Override
    @SuppressWarnings("java:S3655")
    public Mono<RetransmissionResult> requestRetransmission(RetransmissionRequest retransmissionRequest) {
        var permissionRequest = repository.findByPermissionId(retransmissionRequest.permissionId());
        var result = validation.validate(permissionRequest, retransmissionRequest);
        return switch (result) {
            case Success ignored ->
                // permission request must be present if validation successful
                //noinspection OptionalGetWithoutIsPresent
                    pollingFunction.poll(permissionRequest.get(), retransmissionRequest);
            default -> Mono.just(result);
        };
    }
}
