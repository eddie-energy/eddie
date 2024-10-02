package energy.eddie.regionconnector.us.green.button.persistence;

import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReadingPk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, MeterReadingPk> {
}
