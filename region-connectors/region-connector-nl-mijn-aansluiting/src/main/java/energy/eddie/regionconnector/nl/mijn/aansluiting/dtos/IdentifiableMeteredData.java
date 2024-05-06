package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;

import java.util.List;

public record IdentifiableMeteredData(NlPermissionRequest permissionRequest,
                                      List<MijnAansluitingResponse> meteredData) {}
