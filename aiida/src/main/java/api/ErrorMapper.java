package api;

import java.util.List;

public interface ErrorMapper {
    List<EddieApiError> asErrorsList();
}
