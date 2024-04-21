package eddie.energy.europeanmasterdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
class EuropeanMasterDataServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void getPermissionAdministrators() throws IOException {
        List<PermissionAdministrator> permissionAdministrators = List.of(new PermissionAdministrator("country",
                                                                                                     "company",
                                                                                                     "company-id",
                                                                                                     "jumpOffUrl",
                                                                                                     "regionConnector"));

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenReturn(permissionAdministrators);

        EuropeanMasterDataService europeanMasterDataService = new EuropeanMasterDataService(objectMapper);

        List<PermissionAdministrator> result = europeanMasterDataService.getPermissionAdministrators();
        Assertions.assertEquals(permissionAdministrators, result);
    }

    @Test
    void getPermissionAdministrator() throws IOException {
        PermissionAdministrator permissionAdministrator = new PermissionAdministrator("country",
                                                                                      "company",
                                                                                      "company-id",
                                                                                      "jumpOffUrl",
                                                                                      "regionConnector");

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenReturn(List.of(permissionAdministrator));

        EuropeanMasterDataService europeanMasterDataService = new EuropeanMasterDataService(objectMapper);

        PermissionAdministrator result = europeanMasterDataService.getPermissionAdministrator("company-id")
                                                                  .orElseThrow();
        Assertions.assertEquals(permissionAdministrator, result);
    }

    @Test
    void getMeteredDataAdministrators() throws IOException {
        List<MeteredDataAdministrator> meteredDataAdministrators = List.of(new MeteredDataAdministrator("country",
                                                                                                        "company",
                                                                                                        "company-id",
                                                                                                        "websiteUrl",
                                                                                                        "officialContact"));

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class),
                                            ArgumentMatchers.any(JavaType.class)))
               .thenReturn(meteredDataAdministrators);

        EuropeanMasterDataService europeanMasterDataService = new EuropeanMasterDataService(objectMapper);

        List<MeteredDataAdministrator> result = europeanMasterDataService.getMeteredDataAdministrators();
        Assertions.assertEquals(meteredDataAdministrators, result);
    }

    @Test
    void getMeteredDataAdministrator() throws IOException {
        MeteredDataAdministrator meteredDataAdministrator = new MeteredDataAdministrator("country",
                                                                                         "company",
                                                                                         "company-id",
                                                                                         "websiteUrl",
                                                                                         "officialContact");

        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenReturn(List.of(meteredDataAdministrator));

        EuropeanMasterDataService europeanMasterDataService = new EuropeanMasterDataService(objectMapper);

        MeteredDataAdministrator result = europeanMasterDataService.getMeteredDataAdministrator("company-id")
                                                                   .orElseThrow();
        Assertions.assertEquals(meteredDataAdministrator, result);
    }

    @Test
    void fileNotFound() throws IOException {
        Mockito.when(objectMapper.readValue(ArgumentMatchers.any(URL.class), ArgumentMatchers.any(JavaType.class)))
               .thenThrow(new IOException());

        Assertions.assertThrows(FileNotFoundException.class, () -> new EuropeanMasterDataService(objectMapper));
    }
}
