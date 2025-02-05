package energy.eddie.core.services;

import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataServiceTest {

    @Test
    void testGetPermissionAdministrators_returnsListOfAllPermissionAdministrators() {
        // Given
        var md1 = new MockMasterData(
                List.of(getPermissionAdministrator("company1"), getPermissionAdministrator("company2")),
                List.of(getMeteredDataAdministrator("company1"), getMeteredDataAdministrator("company2"))
        );
        var md2 = new MockMasterData(
                List.of(getPermissionAdministrator("company3")),
                List.of(getMeteredDataAdministrator("company3"))
        );
        var collection = new MasterDataService();
        collection.registerMasterData(md1);
        collection.registerMasterData(md2);

        // When
        var res = collection.getPermissionAdministrators();

        // Then
        assertThat(res)
                .containsAll(md1.permissionAdministrators())
                .containsAll(md2.permissionAdministrators());
    }

    @Test
    void testGetPermissionAdministratorById_forKnownPermissionAdministrator_returnsCorrectPermissionAdministrator() {
        // Given
        var md1 = new MockMasterData(
                List.of(getPermissionAdministrator("company1"), getPermissionAdministrator("company2")),
                List.of(getMeteredDataAdministrator("company1"), getMeteredDataAdministrator("company2"))
        );
        var company3 = getPermissionAdministrator("company3");
        var md2 = new MockMasterData(
                List.of(company3),
                List.of(getMeteredDataAdministrator("company3"))
        );
        var collection = new MasterDataService();
        collection.registerMasterData(md1);
        collection.registerMasterData(md2);

        // When
        var res = collection.getPermissionAdministrator("company3");

        // Then
        assertThat(res)
                .isPresent()
                .contains(company3);
    }

    @Test
    void testGetPermissionAdministratorById_forUnknownPermissionAdministrator_returnsCorrectPermissionAdministrator() {
        // Given
        var md1 = new MockMasterData(
                List.of(getPermissionAdministrator("company1"), getPermissionAdministrator("company2")),
                List.of(getMeteredDataAdministrator("company1"), getMeteredDataAdministrator("company2"))
        );
        var md2 = new MockMasterData(
                List.of(getPermissionAdministrator("company3")),
                List.of(getMeteredDataAdministrator("company3"))
        );
        var collection = new MasterDataService();
        collection.registerMasterData(md1);
        collection.registerMasterData(md2);

        // When
        var res = collection.getPermissionAdministrator("unknown");

        // Then
        assertThat(res)
                .isEmpty();
    }

    @Test
    void testGetMeteredDataAdministrators_returnsListOfAllMeteredDataAdministrators() {
        // Given
        var md1 = new MockMasterData(
                List.of(getPermissionAdministrator("company1"), getPermissionAdministrator("company2")),
                List.of(getMeteredDataAdministrator("company1"), getMeteredDataAdministrator("company2"))
        );
        var md2 = new MockMasterData(
                List.of(getPermissionAdministrator("company3")),
                List.of(getMeteredDataAdministrator("company3"))
        );
        var collection = new MasterDataService();
        collection.registerMasterData(md1);
        collection.registerMasterData(md2);

        // When
        var res = collection.getMeteredDataAdministrators();

        // Then
        assertThat(res)
                .containsAll(md1.meteredDataAdministrators())
                .containsAll(md2.meteredDataAdministrators());
    }

    @Test
    void testGetMeteredDataAdministratorById_forKnownMeteredAdministrator_returnsCorrectMeteredAdministrator() {
        // Given
        var md1 = new MockMasterData(
                List.of(getPermissionAdministrator("company1"), getPermissionAdministrator("company2")),
                List.of(getMeteredDataAdministrator("company1"), getMeteredDataAdministrator("company2"))
        );
        var company3 = getMeteredDataAdministrator("company3");
        var md2 = new MockMasterData(
                List.of(getPermissionAdministrator("company3")),
                List.of(company3)
        );
        var collection = new MasterDataService();
        collection.registerMasterData(md1);
        collection.registerMasterData(md2);

        // When
        var res = collection.getMeteredDataAdministrator("company3");

        // Then
        assertThat(res)
                .isPresent()
                .contains(company3);
    }

    @Test
    void testGetMeteredAdministratorById_forUnknownMeteredAdministrator_returnsCorrectMeteredAdministrator() {
        // Given
        var md1 = new MockMasterData(
                List.of(getPermissionAdministrator("company1"), getPermissionAdministrator("company2")),
                List.of(getMeteredDataAdministrator("company1"), getMeteredDataAdministrator("company2"))
        );
        var md2 = new MockMasterData(
                List.of(getPermissionAdministrator("company3")),
                List.of(getMeteredDataAdministrator("company3"))
        );
        var collection = new MasterDataService();
        collection.registerMasterData(md1);
        collection.registerMasterData(md2);

        // When
        var res = collection.getMeteredDataAdministrator("unknown");

        // Then
        assertThat(res)
                .isEmpty();
    }

    private static MeteredDataAdministrator getMeteredDataAdministrator(String id) {
        return new MeteredDataAdministrator("AT", id, id, "http://localhost", "http://localhost", id);
    }

    private static PermissionAdministrator getPermissionAdministrator(String id) {
        return new PermissionAdministrator("AT", id, id, id, "https://localhost", "at-eda");
    }
}