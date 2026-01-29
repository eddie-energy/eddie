// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package eddie.energy.europeanmasterdata;

import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.TypeFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

        when(objectMapper.readValue(any(InputStream.class), any(CollectionType.class)))
                .thenReturn(permissionAdministrators);
        when(objectMapper.getTypeFactory()).thenReturn(TypeFactory.createDefaultInstance());

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

        when(objectMapper.readValue(any(InputStream.class), any(CollectionType.class)))
                .thenReturn(List.of(permissionAdministrator));
        when(objectMapper.getTypeFactory()).thenReturn(TypeFactory.createDefaultInstance());

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

        when(objectMapper.readValue(any(InputStream.class), any(CollectionType.class)))
                .thenReturn(meteredDataAdministrators);
        when(objectMapper.getTypeFactory()).thenReturn(TypeFactory.createDefaultInstance());

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

        when(objectMapper.readValue(any(InputStream.class), any(CollectionType.class)))
                .thenReturn(List.of(meteredDataAdministrator));
        when(objectMapper.getTypeFactory()).thenReturn(TypeFactory.createDefaultInstance());

        JsonMasterData jsonMasterData = new JsonMasterData(objectMapper);

        MeteredDataAdministrator result = jsonMasterData.getMeteredDataAdministrator("company-id")
                                                        .orElseThrow();
        Assertions.assertEquals(meteredDataAdministrator, result);
    }

    @Test
    void fileNotFound() {
        when(objectMapper.readValue(any(InputStream.class), any(JavaType.class)))
                .thenThrow(JacksonException.wrapWithPath(new Exception("message"), new Object(), 0));
        when(objectMapper.getTypeFactory()).thenReturn(TypeFactory.createDefaultInstance());

        Assertions.assertThrows(FileNotFoundException.class, () -> new JsonMasterData(objectMapper));
    }
}
