package energy.eddie.api.agnostic.master.data;

import java.util.List;
import java.util.Optional;

public interface MasterDataCollection {
    List<PermissionAdministrator> getPermissionAdministrators();

    Optional<PermissionAdministrator> getPermissionAdministrator(String id);

    List<MeteredDataAdministrator> getMeteredDataAdministrators();

    Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id);
}
