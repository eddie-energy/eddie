package energy.eddie.regionconnector.at.eda;

import jakarta.annotation.Nullable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

public class InMemoryEdaIdMapper implements EdaIdMapper {

    private final ConcurrentMap<String, MappingInfo> requestIdToMappingInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MappingInfo> consentIdToMappingInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MappingInfo> conversationIdToMappingInfo = new ConcurrentHashMap<>();


    @Override
    public void addMappingInfo(String conversationId, String requestId, MappingInfo mappingInfo) {
        requireNonNull(conversationId);
        requireNonNull(requestId);
        requireNonNull(mappingInfo);

        requestIdToMappingInfo.put(requestId, mappingInfo);
        conversationIdToMappingInfo.put(conversationId, mappingInfo);
    }

    @Override
    public boolean addMappingForConsentId(String consentId, String requestId) {
        requireNonNull(consentId);
        requireNonNull(requestId);

        var mappingInfo = requestIdToMappingInfo.get(requestId);
        if (mappingInfo == null) {
            return false;
        }

        consentIdToMappingInfo.put(consentId, mappingInfo);
        return true;
    }

    @Override
    public Optional<MappingInfo> getMappingInfoForConversationIdOrRequestID(String conversationId, @Nullable String requestId) {
        var mappingInfo = conversationIdToMappingInfo.get(conversationId);

        if (mappingInfo == null && requestId != null) {
            mappingInfo = requestIdToMappingInfo.get(requestId);
        }

        return Optional.ofNullable(mappingInfo);
    }

    @Override
    public Optional<MappingInfo> getMappingInfoForConsentId(String consentId) {
        requireNonNull(consentId);

        return Optional.ofNullable(consentIdToMappingInfo.get(consentId));
    }
}
