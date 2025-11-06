package energy.eddie.outbound.rest.dto;

import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "VHDEnvelopes")
@SuppressWarnings("java:S101")
public class ValidatedHistoricalDataMarketDocumentsV1_04 extends CimCollection<VHDEnvelope> {
    public ValidatedHistoricalDataMarketDocumentsV1_04(List<VHDEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public ValidatedHistoricalDataMarketDocumentsV1_04() {
        super();
    }
}
