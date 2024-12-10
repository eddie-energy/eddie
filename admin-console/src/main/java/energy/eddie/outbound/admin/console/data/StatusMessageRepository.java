package energy.eddie.outbound.admin.console.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StatusMessageRepository extends JpaRepository<StatusMessage, Long> {
    @Query(value = """
            SELECT DISTINCT ON (permission_id) *
                                FROM admin_console.status_messages
                                ORDER BY permission_id, start_date DESC, id DESC
            """, nativeQuery = true)
    List<StatusMessage> findLatestStatusMessageForAllPermissions();

    List<StatusMessage> findByPermissionIdOrderByStartDateDescIdDesc(String permissionId);
}
