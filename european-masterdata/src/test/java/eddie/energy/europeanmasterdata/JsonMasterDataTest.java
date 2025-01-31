package eddie.energy.europeanmasterdata;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class JsonMasterDataTest {
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void getPermissionAdministrators() throws IOException {
        List<PermissionAdministrator> permissionAdministrators = List.of(new PermissionAdministrator("country",
                                                                                                     "company",
                                                                                                     "name",
                                                                                                     "company-id",
                                                                                                     "jumpOffUrl",
                                                                                                     "regionConnector"));

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenReturn(permissionAdministrators);

        JsonMasterData jsonMasterData = new JsonMasterData(objectMapper);

        List<PermissionAdministrator> result = jsonMasterData.permissionAdministrators();
        Assertions.assertEquals(permissionAdministrators, result);
    }

    @Test
    void getPermissionAdministrator() throws IOException {
        PermissionAdministrator permissionAdministrator = new PermissionAdministrator("country",
                                                                                      "company",
                                                                                      "name",
                                                                                      "company-id",
                                                                                      "jumpOffUrl",
                                                                                      "regionConnector");

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenReturn(List.of(permissionAdministrator));

        JsonMasterData jsonMasterData = new JsonMasterData(objectMapper);

        PermissionAdministrator result = jsonMasterData.getPermissionAdministrator("company-id")
                                                       .orElseThrow();
        Assertions.assertEquals(permissionAdministrator, result);
    }

    @Test
    void getMeteredDataAdministrators() throws IOException {
        List<MeteredDataAdministrator> meteredDataAdministrators = List.of(new MeteredDataAdministrator("country",
                                                                                                        "company",
                                                                                                        "company-id",
                                                                                                        "websiteUrl",
                                                                                                        "officialContact",
                                                                                                        "permissionAdministrator"));

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class),
                                            ArgumentMatchers.any(JavaType.class)))
               .thenReturn(meteredDataAdministrators);

        JsonMasterData jsonMasterData = new JsonMasterData(objectMapper);

        List<MeteredDataAdministrator> result = jsonMasterData.meteredDataAdministrators();
        Assertions.assertEquals(meteredDataAdministrators, result);
    }

    @Test
    void getMeteredDataAdministrator() throws IOException {
        MeteredDataAdministrator meteredDataAdministrator = new MeteredDataAdministrator("country",
                                                                                         "company",
                                                                                         "company-id",
                                                                                         "websiteUrl",
                                                                                         "officialContact",
                                                                                         "permissionAdministrator");

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenReturn(List.of(meteredDataAdministrator));

        JsonMasterData jsonMasterData = new JsonMasterData(objectMapper);

        MeteredDataAdministrator result = jsonMasterData.getMeteredDataAdministrator("company-id")
                                                        .orElseThrow();
        Assertions.assertEquals(meteredDataAdministrator, result);
    }

    @Test
    void fileNotFound() throws IOException {
        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenThrow(new IOException());

        Assertions.assertThrows(FileNotFoundException.class, () -> new JsonMasterData(objectMapper));
    }
}
