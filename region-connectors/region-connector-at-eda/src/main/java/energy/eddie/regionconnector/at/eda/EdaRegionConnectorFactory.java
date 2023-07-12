package energy.eddie.regionconnector.at.eda;

import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.RegionConnectorProvisioningException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.ConfigAtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.ConfigPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import jakarta.xml.bind.JAXBException;
import org.eclipse.microprofile.config.Config;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class EdaRegionConnectorFactory implements RegionConnectorFactory {

    @Override
    // sonarcloud complains about not using try-with-resources, but using it would close the adapter upon returning, which is not what we want
    @SuppressWarnings("java:S2095")
    public RegionConnector create(Config config) throws RegionConnectorProvisioningException {
        requireNonNull(config);
        AtConfiguration atConfiguration = new ConfigAtConfiguration(config);

        EdaIdMapper idMapper = new InMemoryEdaIdMapper();

        PontonXPAdapterConfiguration pontonConfiguration = new ConfigPontonXPAdapterConfiguration(config);

        try {
            var adapter = new PontonXPAdapter(pontonConfiguration);
            return new EdaRegionConnector(atConfiguration, adapter, idMapper);
        } catch (IOException | ConnectionException | JAXBException | TransmissionException e) {
            throw new RegionConnectorProvisioningException(e);
        }
    }
}
