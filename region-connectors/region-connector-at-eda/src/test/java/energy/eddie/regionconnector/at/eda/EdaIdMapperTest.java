package energy.eddie.regionconnector.at.eda;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

abstract class EdaIdMapperTest {

    EdaIdMapper edaIdMapper; // add @BeforeEach in concrete class

    @AfterEach
    void tearDown() {
        edaIdMapper = null;
    }

    @Test
    void addMappingInfo_throwsIfParametersAreNull() {
        assertThrows(NullPointerException.class, () -> edaIdMapper.addMappingInfo(null, "requestId", new MappingInfo("permissionId", "connectionId")));
        assertThrows(NullPointerException.class, () -> edaIdMapper.addMappingInfo("conversationId", null, new MappingInfo("permissionId", "connectionId")));
        assertThrows(NullPointerException.class, () -> edaIdMapper.addMappingInfo("conversationId", "requestId", null));
    }

    @Test
    void addMappingInfo_addsMapping() {
        var mappingInfo = new MappingInfo("permissionId", "connectionId");
        var conversationId = "conversationId";
        var requestId = "requestId";

        edaIdMapper.addMappingInfo(conversationId, requestId, mappingInfo);

        var mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID(conversationId, requestId);
        assertTrue(mappingInfoOptional.isPresent());
        assertEquals(mappingInfo, mappingInfoOptional.orElseThrow());
    }

    @Test
    void addMappingInfo_OverridesExistingMapping() {
        var mappingInfo = new MappingInfo("permissionId", "connectionId");
        var mappingInfo2 = new MappingInfo("permissionId2", "connectionId2");
        var conversationId = "conversationId";
        var requestId = "requestId";

        edaIdMapper.addMappingInfo(conversationId, requestId, mappingInfo);
        var mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID(conversationId, requestId);
        assertTrue(mappingInfoOptional.isPresent());
        assertEquals(mappingInfo, mappingInfoOptional.orElseThrow());

        edaIdMapper.addMappingInfo(conversationId, requestId, mappingInfo2);
        mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID(conversationId, requestId);
        assertTrue(mappingInfoOptional.isPresent());
        assertEquals(mappingInfo2, mappingInfoOptional.orElseThrow());
    }

    @Test
    void getMappingInfoForConversationIdOrRequestID_MappingInfoDoesNotExist_ReturnsEmpty() {
        var mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID("nomatch", "nomatch");
        assertFalse(mappingInfoOptional.isPresent());
    }

    @Test
    void getMappingInfoForConversationIdOrRequestID_MappingInfoDoesNotExistRequestIdNull_ReturnsEmpty() {
        var mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID("nomatch", null);
        assertFalse(mappingInfoOptional.isPresent());
    }

    @Test
    void getMappingInfoForConversationIdOrRequestID_MappingInfoExists_ReturnsMappingInfo() {
        var mappingInfo = new MappingInfo("permissionId", "connectionId");
        var conversationId = "conversationId";
        var requestId = "requestId";

        edaIdMapper.addMappingInfo(conversationId, requestId, mappingInfo);

        var mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID(conversationId, requestId);
        assertTrue(mappingInfoOptional.isPresent());
        assertEquals(mappingInfo, mappingInfoOptional.orElseThrow());
    }

    @Test
    void getMappingInfoForConversationIdOrRequestID_MappingInfoExistsRequestIdIsNull_ReturnsMappingInfo() {
        var mappingInfo = new MappingInfo("permissionId", "connectionId");
        var conversationId = "conversationId";
        var requestId = "requestId";

        edaIdMapper.addMappingInfo(conversationId, requestId, mappingInfo);

        var mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID(conversationId, null);
        assertTrue(mappingInfoOptional.isPresent());
        assertEquals(mappingInfo, mappingInfoOptional.orElseThrow());
    }

    @Test
    void getMappingInfoForConversationIdOrRequestID_MappingInfoExistsConversationIdHasNoMatch_ReturnsMappingInfo() {
        var mappingInfo = new MappingInfo("permissionId", "connectionId");
        var conversationId = "conversationId";
        var requestId = "requestId";

        edaIdMapper.addMappingInfo(conversationId, requestId, mappingInfo);

        var mappingInfoOptional = edaIdMapper.getMappingInfoForConversationIdOrRequestID("nomatch", requestId);
        assertTrue(mappingInfoOptional.isPresent());
        assertEquals(mappingInfo, mappingInfoOptional.orElseThrow());
    }

    @Test
    void addMappingForConsentId_throwsIfParametersAreNull() {
        assertThrows(NullPointerException.class, () -> edaIdMapper.addMappingForConsentId(null, "requestId"));
        assertThrows(NullPointerException.class, () -> edaIdMapper.addMappingForConsentId("conversationId", null));
    }

    @Test
    void addMappingForConsentId_NoMappingExists_ReturnsFalse() {
        var conversationId = "conversationId";
        var requestId = "requestId";

        var result = edaIdMapper.addMappingForConsentId(conversationId, requestId);

        assertFalse(result);
    }

    @Test
    void addMappingForConsentId_MappingExists_ReturnsTrue() {
        var conversationId = "conversationId";
        var requestId = "requestId";

        edaIdMapper.addMappingInfo(conversationId, requestId, new MappingInfo("permissionId", "connectionId"));
        var result = edaIdMapper.addMappingForConsentId(conversationId, requestId);

        assertTrue(result);
    }

    @Test
    void getMappingInfoForConsentId_MappingExists_ReturnsMappingInfo() {
        var mappingInfo = new MappingInfo("permissionId", "connectionId");
        var conversationId = "conversationId";
        var requestId = "requestId";

        edaIdMapper.addMappingInfo(conversationId, requestId, mappingInfo);
        edaIdMapper.addMappingForConsentId(conversationId, requestId);

        var mappingInfoOptional = edaIdMapper.getMappingInfoForConsentId(conversationId);
        assertTrue(mappingInfoOptional.isPresent());
        assertEquals(mappingInfo, mappingInfoOptional.orElseThrow());
    }

    @Test
    void getMappingInfoForConsentId_MappingDoesNotExist_ReturnsEmpty() {
        var mappingInfoOptional = edaIdMapper.getMappingInfoForConsentId("nomatch");
        assertFalse(mappingInfoOptional.isPresent());
    }

}