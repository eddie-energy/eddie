// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata;

import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.EdaMasterData01p32InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33.EdaMasterData01p33InboundMessageFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarshallerConfig.class)
class MultiMasterDataInboundMessageFactoryTest {
    @Autowired
    private Jaxb2Marshaller marshaller;

    @ParameterizedTest
    @ValueSource(strings = {"xsd/masterdata/_01p33/masterdata.xml", "xsd/masterdata/_01p32/masterdata.xml"})
    void parseInputStream_forVersion_doesNotThrow(String masterdata) throws IOException {
        // Given
        var multiFactory = new MultiMasterDataInboundMessageFactory(Set.of(
                new EdaMasterData01p32InboundMessageFactory(marshaller),
                new EdaMasterData01p33InboundMessageFactory(marshaller)
        ));

        ClassLoader classLoader = getClass().getClassLoader();
        try (var inputStream = classLoader.getResourceAsStream(masterdata)) {
            // When & Then
            assertDoesNotThrow(() -> multiFactory.parseInputStream(inputStream));
        }
    }
}