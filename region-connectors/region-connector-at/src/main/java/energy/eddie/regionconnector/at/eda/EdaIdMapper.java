package energy.eddie.regionconnector.at.eda;

import jakarta.annotation.Nullable;

import java.util.Optional;

/**
 * Stores permissionId and connectionId as well as information needed to retrieve them.
 */
public interface EdaIdMapper {

    /**
     * Adds mapping info for the given conversationId and requestId.
     * If the mapping info already exists, it will be overwritten.
     * The mapping info can be retrieved by calling {@link #getMappingInfoForConversationIdOrRequestID(String, String)}.
     * @param conversationId can be used to retrieve the mapping info as long as the messages from EDA are sent in the same process. Which should be the case for HistoricalMeteringData and MasterData but nor MeteringData.
     * @param requestId can be used to retrieve the mapping info for all EDA messages that contain the requestId. Should only be the messages that are sent for the consent requests.
     * @param mappingInfo the mapping info to be added
     */
    void addMappingInfo(String conversationId, String requestId, MappingInfo mappingInfo);

    /**
     * Adds mapping info for the given consentId.
     * The requestId is used to look up the existing mapping info.
     * The mapping info can be retrieved by calling {@link #getMappingInfoForConsentId(String)}.
     * @param consentId uniquely identifies the given consent
     * @param requestId can be used to retrieve the mapping info for all EDA messages that contain the requestId. Should only be the messages that are sent for the consent requests.
     * @return true if the mapping was added successfully, false if no mapping for the given requestId exists
     */
    boolean addMappingForConsentId(String consentId,String requestId);

    /**
     * Retrieves the mapping info for the given conversationId or requestId.
     * @param conversationId can be used to retrieve the mapping info as long as the messages from EDA are sent in the same process. Which should be the case for HistoricalMeteringData and MasterData but nor MeteringData.
     * @param requestId can be used to retrieve the mapping info for all EDA messages that contain the requestId. Should only be the messages that are sent for the consent requests.
     * @return the mapping info if it exists, empty otherwise
     */
    Optional<MappingInfo> getMappingInfoForConversationIdOrRequestID(String conversationId, @Nullable String requestId);

    /**
     * Retrieves the mapping info for the given consentId.
     * @param consentId uniquely identifies the given consent
     * @return the mapping info if it exists, empty otherwise
     */
    Optional<MappingInfo> getMappingInfoForConsentId(String consentId);
}