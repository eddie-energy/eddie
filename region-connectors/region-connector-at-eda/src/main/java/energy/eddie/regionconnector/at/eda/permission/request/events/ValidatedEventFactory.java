package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.MessageId;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.utils.CMRequestId;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class ValidatedEventFactory {
    private final AtConfiguration configuration;

    public ValidatedEventFactory(AtConfiguration configuration) {this.configuration = configuration;}

    public ValidatedEvent createValidatedEvent(
            String permissionId,
            LocalDate start,
            @Nullable LocalDate end,
            @Nullable AllowedGranularity granularity
    ) {
        ZonedDateTime created = ZonedDateTime.now(AT_ZONE_ID);
        var messageId = new MessageId(configuration.eligiblePartyId(), created).toString();
        var cmRequestId = new CMRequestId(messageId).toString();

        return new ValidatedEvent(
                permissionId,
                start,
                end,
                granularity,
                cmRequestId,
                messageId
        );
    }
}
