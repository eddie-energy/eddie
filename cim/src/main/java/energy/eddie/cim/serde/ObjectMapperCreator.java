// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;


import energy.eddie.cim.agnostic.DataSourceInformation;
import energy.eddie.cim.agnostic.SimpleDataSourceInformation;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import java.util.UUID;

class ObjectMapperCreator {
    private ObjectMapperCreator() {
        // Utility Class
    }

    /**
     * Creates an object mapper instance that's specific for the given format.
     *
     * @param format The format that the object mapper should be able to serialize and deserialize.
     * @return object mapper instance for given format.
     */
    public static ObjectMapper create(SerializationFormat format) {
        MapperBuilder<?, ?> builder = switch (format) {
            case XML -> XmlMapper.builder();
            case JSON -> JsonMapper.builder();
        };

        var simpleModule = new SimpleModule()
                .addAbstractTypeMapping(DataSourceInformation.class, SimpleDataSourceInformation.class)
                .addSerializer(UUID.class, new ToStringSerializer(UUID.class));
        return builder.addModule(new JakartaXmlBindAnnotationModule())
                      .addModule(simpleModule)
                      .build();
    }
}
