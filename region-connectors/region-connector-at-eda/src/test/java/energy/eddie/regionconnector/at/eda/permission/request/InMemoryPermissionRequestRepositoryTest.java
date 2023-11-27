package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPermissionRequestRepositoryTest {

    private static Stream<Arguments> inTimeFrameDateSource() {
        LocalDate baseDateFrom = LocalDate.of(2023, 1, 1);
        LocalDate baseDateTo = baseDateFrom.plusDays(30);
        return Stream.of(
                Arguments.of(baseDateFrom, baseDateTo, baseDateFrom, "start of the timeframe"),
                Arguments.of(baseDateFrom, baseDateTo, baseDateFrom.plusDays(15), "middle of the timeframe"),
                Arguments.of(baseDateFrom, baseDateTo, baseDateTo, "end of the timeframe")
        );
    }

    private static Stream<Arguments> outsideTimeFrameDateSource() {
        LocalDate baseDateFrom = LocalDate.of(2023, 1, 1);
        LocalDate baseDateTo = baseDateFrom.plusDays(30);
        return Stream.of(
                Arguments.of(baseDateFrom, baseDateTo, baseDateFrom.minusMonths(1), "before start of the timeframe"),
                Arguments.of(baseDateFrom, baseDateTo, baseDateFrom.minusDays(1), "just before start of the timeframe"),
                Arguments.of(baseDateFrom, baseDateTo, baseDateTo.plusDays(1), "just after the end the timeframe"),
                Arguments.of(baseDateFrom, baseDateTo, baseDateTo.plusMonths(1), "after end of the timeframe")
        );
    }

    @Test
    void givenNewRepository_whenSaveAndFindByPermissionId_thenPermissionRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId");

        // When
        repository.save(request);
        Optional<AtPermissionRequest> foundRequest = repository.findByPermissionId("permissionId");

        // Then
        assertTrue(foundRequest.isPresent());
    }

    @Test
    void givenRepositoryWithRequests_whenFindByPermissionIdNonExistent_thenNoRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId");
        repository.save(request);

        // When
        Optional<AtPermissionRequest> foundRequest = repository.findByPermissionId("nonExistentId");

        // Then
        assertFalse(foundRequest.isPresent());
    }

    @Test
    void givenRepositoryWithMultipleRequests_whenFindByPermissionId_thenCorrectRequestsFound() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request1 = new SimplePermissionRequest("permissionId1", "connectionId1");
        var request2 = new SimplePermissionRequest("permissionId2", "connectionId2");
        repository.save(request1);
        repository.save(request2);

        // When
        Optional<AtPermissionRequest> foundRequest1 = repository.findByPermissionId("permissionId1");

        // Then
        assertEquals("connectionId1", foundRequest1.get().connectionId());
    }

    @Test
    void findByConversationIdAndCmRequestId_returnsEmptyOptional_ifIdsDoNotMatch() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        Optional<AtPermissionRequest> foundRequest = repository.findByConversationIdOrCMRequestId("asdf", "hjkl");

        // Then
        assertTrue(foundRequest.isEmpty());
    }

    @Test
    void findByConversationIdAndCmRequestId_returnsRequest_ifConversationIdMatches() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        Optional<AtPermissionRequest> foundRequest = repository.findByConversationIdOrCMRequestId("cid1", "hjkl");

        // Then
        assertEquals(request, foundRequest.get());
    }

    @Test
    void findByConversationIdAndCmRequestId_returnsRequest_ifCmRequestIdMatches() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        Optional<AtPermissionRequest> foundRequest = repository.findByConversationIdOrCMRequestId("asdf", "rid1");

        // Then
        assertEquals(request, foundRequest.get());
    }

    // method source for dates

    @Test
    void removeByPermissionId_withNonExistentKey_returnsFalse() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        boolean found = repository.removeByPermissionId("asdf");

        // Then
        assertFalse(found);
    }

    @Test
    void removeByPermissionId_withExistingKey_returnsTrue() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        boolean found = repository.removeByPermissionId("permissionId");

        // Then
        assertTrue(found);
    }

    @DisplayName("Check matching permission requests for ")
    @ParameterizedTest(name = "{displayName} {3}")
    @MethodSource("inTimeFrameDateSource")
    void findByMeteringPointIdAndDate_givenDifferentDates_returnsMatch(LocalDate dateFrom, LocalDate dateTo, LocalDate retrievalDate, String description) {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        String meteringPointId = "meteringPointId";
        var request = new SimplePermissionRequest(
                "permissionId", "connectionId1",
                "dataNeedId",
                "rid1",
                "cid1",
                "dsoId",
                Optional.of(meteringPointId),
                dateFrom,
                Optional.of(dateTo),
                null);
        repository.save(request);

        // When
        var permissionRequests = repository.findByMeteringPointIdAndDate(meteringPointId, retrievalDate);

        // Then
        assertEquals(1, permissionRequests.size());
    }

    @DisplayName("Check matching permission requests for ")
    @ParameterizedTest(name = "{displayName} {3}")
    @MethodSource("outsideTimeFrameDateSource")
    void findByMeteringPointIdAndDate_givenDifferentDatesOutsideOfTimeFrame_returnsEmpty(LocalDate dateFrom, LocalDate dateTo, LocalDate retrievalDate, String description) {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        String meteringPointId = "meteringPointId";
        var request = new SimplePermissionRequest(
                "permissionId", "connectionId1",
                "dataNeedId",
                "rid1",
                "cid1",
                "dsoId",
                Optional.of(meteringPointId),
                dateFrom,
                Optional.of(dateTo),
                null);
        repository.save(request);

        // When
        var permissionRequests = repository.findByMeteringPointIdAndDate(meteringPointId, retrievalDate);

        // Then
        assertTrue(permissionRequests.isEmpty());
    }

    @Test
    void findByMeteringPointIdAndDate_withMultipleMatches_returnsMatches() {
        // Given
        LocalDate dateFrom = LocalDate.of(2023, 1, 1);
        LocalDate dateTo = dateFrom.plusDays(30);
        LocalDate retrievalDate = dateFrom.plusDays(15);
        var repository = new InMemoryPermissionRequestRepository();
        String meteringPointId = "meteringPointId";
        var request = new SimplePermissionRequest(
                "permissionId",
                "connectionId1",
                "dataNeedId",
                "rid1",
                "cid1",
                "dsoId",
                Optional.of(meteringPointId),
                dateFrom,
                Optional.of(dateTo),
                null);
        var request2 = new SimplePermissionRequest(
                "permissionId2",
                "connectionId2",
                "dataNeedId",
                "rid2",
                "cid2",
                "dsoId",
                Optional.of(meteringPointId),
                dateFrom,
                Optional.of(dateTo),
                null);

        repository.save(request);
        repository.save(request2);

        // When
        var permissionRequests = repository.findByMeteringPointIdAndDate(meteringPointId, retrievalDate);

        // Then
        assertEquals(2, permissionRequests.size());
    }


    @Test
    void findByMeteringPointIdAndDate_withMultipleRequests_returnsOnlyMatches() {
        // Given
        LocalDate dateFrom = LocalDate.of(2023, 1, 1);
        LocalDate dateTo = dateFrom.plusDays(30);
        LocalDate retrievalDate = dateFrom.plusDays(15);
        var repository = new InMemoryPermissionRequestRepository();
        String meteringPointId = "meteringPointId";
        var request = new SimplePermissionRequest(
                "permissionId",
                "connectionId1",
                "dataNeedId",
                "rid1",
                "cid1",
                "dsoId",
                Optional.of(meteringPointId),
                dateFrom,
                Optional.of(dateTo),
                null);
        var request2 = new SimplePermissionRequest(
                "permissionId2",
                "connectionId2",
                "dataNeedId",
                "rid2",
                "cid2",
                "dsoId",
                Optional.of("otherMeteringPointId"),
                dateFrom,
                Optional.of(dateTo),
                null);

        repository.save(request);
        repository.save(request2);

        // When
        var permissionRequests = repository.findByMeteringPointIdAndDate(meteringPointId, retrievalDate);

        // Then
        assertEquals(1, permissionRequests.size());
    }
}