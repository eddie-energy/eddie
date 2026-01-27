package energy.eddie.outbound.rest.dto.v1_06;

import energy.eddie.cim.v1_06.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.dto.CimCollection;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlType(name = "NearRealTimeDataMarketDocumentsV1_06")
@XmlRootElement(name = "NearRealTimeDataMarketDocuments")
public class NearRealTimeDataMarketDocuments extends CimCollection<RTDEnvelope> {
    public NearRealTimeDataMarketDocuments(List<RTDEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public NearRealTimeDataMarketDocuments() {
        super();
    }
}
