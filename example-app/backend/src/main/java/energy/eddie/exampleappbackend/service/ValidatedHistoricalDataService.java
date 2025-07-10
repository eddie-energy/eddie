package energy.eddie.exampleappbackend.service;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class ValidatedHistoricalDataService {
    // Runtime Cache in case the VHD Message is received before Permission (use more sophisticated cache for production environments)
    private final Map<String, ValidatedHistoricalDataEnvelope> cache = new LinkedHashMap<>();

    public void pufferValidateHistoricalDataEnvelope(ValidatedHistoricalDataEnvelope message) {
        if (message == null || message.getMessageDocumentHeader() == null || message.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation() == null) {
            log.warn("Cannot cache invalid message!");
            return;
        }
        cache.put(message.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation().getPermissionid(), message);
    }

    public void handleCachedMessageWithEddiePermissionId(String eddiePermissionId) {
        if (cache.containsKey(eddiePermissionId)) {
            handleCachedMessageWithEddiePermissionId(eddiePermissionId);
        }
    }

    public void handleValidatedHistoricalDataEnvelope(ValidatedHistoricalDataEnvelope message) {

    }
}
