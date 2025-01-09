package energy.eddie.regionconnector.at.eda;

import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.EdaMasterData01p32InboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.IOException;

public class EdaResourceLoader {
    private static final Jaxb2Marshaller marshaller = new AtEdaBeanConfig().jaxb2Marshaller();

    public static EdaMasterData loadEdaMasterData() throws IOException {
        return load("xsd/masterdata/_01p32/masterdata.xml");
    }

    public static EdaMasterData loadEdaMasterDataForCompany() throws IOException {
        return load("xsd/masterdata/_01p32/masterdata-forCompany.xml");
    }

    public static EdaMasterData loadEdaMasterDataWithoutMeteringPointData() throws IOException {
        return load("xsd/masterdata/_01p32/masterdata-without-metering-point-data.xml");
    }
    private static EdaMasterData load(String name) throws IOException {
        var classLoader = EdaResourceLoader.class.getClassLoader();
        try (var inputStream = classLoader.getResourceAsStream(name)) {
            var factory = new EdaMasterData01p32InboundMessageFactory(marshaller);
            return factory.parseInputStream(inputStream);
        }
    }
}
