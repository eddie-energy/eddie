package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke;

import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaCMRevokeInboundMessageFactory extends PontonMessageFactory {
    EdaCMRevoke parseInputStream(InputStream inputStream);
}
