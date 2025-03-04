package energy.eddie.outbound.admin.console.data;

import java.util.List;

public record PermissionPage(List<StatusMessageDTO> permissions, long totalElements) {

}
