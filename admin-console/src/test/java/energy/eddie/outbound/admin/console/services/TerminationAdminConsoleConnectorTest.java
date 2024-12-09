package energy.eddie.outbound.admin.console.services;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminationAdminConsoleConnectorTest {

    @SuppressWarnings("resource")
    @Test
    void testTerminate_emitsTerminationDocument() {
        // Given
        var terminationConnector = new TerminationAdminConsoleConnector();

        // When
        terminationConnector.terminate("pid", "at-eda");

        // Then
        StepVerifier.create(terminationConnector.getTerminationMessages())
                    .then(terminationConnector::close)
                    .assertNext(res -> assertAll(
                            () -> assertEquals("pid", res.value().getPermissionMarketDocument().getMRID()),
                            () -> assertEquals("at-eda",
                                               res.value()
                                                  .getPermissionMarketDocument()
                                                  .getPermissionList()
                                                  .getPermissions()
                                                  .getFirst()
                                                  .getMktActivityRecordList()
                                                  .getMktActivityRecords()
                                                  .getFirst()
                                                  .getType())
                    ))
                    .verifyComplete();
    }
}