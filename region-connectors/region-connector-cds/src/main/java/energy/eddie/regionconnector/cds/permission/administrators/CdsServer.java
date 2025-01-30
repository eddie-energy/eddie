package energy.eddie.regionconnector.cds.permission.administrators;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.net.URI;

@Entity(name = "cds_server")
public class CdsServer {
    @Id
    private final URI uri;

    public CdsServer(URI uri) {this.uri = uri;}

    @SuppressWarnings("NullAway")
    protected CdsServer() {
        uri = null;
    }
}
