package energy.eddie.regionconnector.shared.web;

import energy.eddie.api.agnostic.EddieApiError;

import java.util.List;

public interface ErrorMapper {
    List<EddieApiError> asErrorsList();
}
