// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.serializer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

public class LocalDateToEpochSerializer extends StdSerializer<LocalDate> {
    protected LocalDateToEpochSerializer() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(
            LocalDate value,
            tools.jackson.core.JsonGenerator gen,
            SerializationContext provider
    ) throws JacksonException {
        long timestamp = value.atStartOfDay(ZONE_ID_SPAIN).toInstant().toEpochMilli();
        gen.writeNumber(timestamp);
    }
}
