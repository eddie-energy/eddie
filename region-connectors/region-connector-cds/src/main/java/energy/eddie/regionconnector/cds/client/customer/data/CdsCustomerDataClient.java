package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.master.data.CdsServer;
import org.springframework.web.reactive.function.client.WebClient;

public class CdsCustomerDataClient {
    private final CdsServer cdsServer;
    private final WebClient webClient;

    public CdsCustomerDataClient(CdsServer cdsServer, WebClient webClient) {
        this.cdsServer = cdsServer;
        this.webClient = webClient;
    }

}
