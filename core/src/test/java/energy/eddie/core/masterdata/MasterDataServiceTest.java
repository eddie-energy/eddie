package energy.eddie.core.masterdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterDataServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void getPermissionAdministrators() throws IOException {
        List<PermissionAdministrator> permissionAdministrators = List.of(new PermissionAdministrator("country", "company", "company-id", "jumpOffUrl", "regionConnector"));

        when(objectMapper.readValue(any(URL.class), any(TypeReference.class))).thenReturn(permissionAdministrators);

        MasterDataService masterDataService = new MasterDataService(objectMapper);

        List<PermissionAdministrator> result = masterDataService.getPermissionAdministrators();
        assertEquals(permissionAdministrators, result);
    }

    @Test
    void getPermissionAdministrator() throws IOException {
        PermissionAdministrator permissionAdministrator = new PermissionAdministrator("country", "company", "company-id", "jumpOffUrl", "regionConnector");

        when(objectMapper.readValue(any(URL.class), any(TypeReference.class))).thenReturn(List.of(permissionAdministrator));

        MasterDataService masterDataService = new MasterDataService(objectMapper);

        PermissionAdministrator result = masterDataService.getPermissionAdministrator("company-id").orElseThrow();
        assertEquals(permissionAdministrator, result);
    }

    @Test
    void getMeteredDataAdministrators() throws IOException {
        List<MeteredDataAdministrator> meteredDataAdministrators = List.of(new MeteredDataAdministrator("country", "company", "company-id", "websiteUrl", "officialContact"));

        when(objectMapper.readValue(any(URL.class), any(TypeReference.class))).thenReturn(meteredDataAdministrators);

        MasterDataService masterDataService = new MasterDataService(objectMapper);

        List<MeteredDataAdministrator> result = masterDataService.getMeteredDataAdministrators();
        assertEquals(meteredDataAdministrators, result);
    }

    @Test
    void getMeteredDataAdministrator() throws IOException {
        MeteredDataAdministrator meteredDataAdministrator = new MeteredDataAdministrator("country", "company", "company-id", "websiteUrl", "officialContact");

        when(objectMapper.readValue(any(URL.class), any(TypeReference.class))).thenReturn(List.of(meteredDataAdministrator));

        MasterDataService masterDataService = new MasterDataService(objectMapper);

        MeteredDataAdministrator result = masterDataService.getMeteredDataAdministrator("company-id").orElseThrow();
        assertEquals(meteredDataAdministrator, result);
    }

    @Test
    void fileNotFound() throws IOException {
        when(objectMapper.readValue(any(URL.class), any(TypeReference.class))).thenThrow(new IOException());

        assertThrows(FileNotFoundException.class, () -> new MasterDataService(objectMapper));
    }
}