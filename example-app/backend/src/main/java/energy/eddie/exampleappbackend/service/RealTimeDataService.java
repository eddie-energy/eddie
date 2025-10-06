package energy.eddie.exampleappbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v1_04.rtd.QuantityTypeKind;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.exampleappbackend.config.ExampleAppConfig;
import energy.eddie.exampleappbackend.model.db.TimeSeries;
import energy.eddie.exampleappbackend.model.db.TimeSeriesList;
import energy.eddie.exampleappbackend.persistence.PermissionRepository;
import energy.eddie.exampleappbackend.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealTimeDataService {
    private static final String RTD_WEBSOCKET_TOPIC_PREFIX = "/topic/real-time-data/";
    private final PermissionRepository permissionRepository;
    private final WebSocketService webSocketService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final ExampleAppConfig exampleAppConfig;

    public String createNewSecretForRTDWebSocketTopic(Long permissionId) {
        var userId = authService.getCurrentUserId();
        var permission = permissionRepository.findById(permissionId)
                                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!permission.getUserId().equals(userId.toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return webSocketService.createNewSecretForTopic(RTD_WEBSOCKET_TOPIC_PREFIX + permissionId);
    }

    @Transactional
    public void handelRealTimeDataEnvelope(RTDEnvelope rtdEnvelope) {
        var eddiePermissionId = rtdEnvelope.getMessageDocumentHeaderMetaInformationPermissionId();
        permissionRepository.findByEddiePermissionId(eddiePermissionId).ifPresentOrElse((permission) -> {
            List<TimeSeries> newTimeSeries;
            if (permission.getTimeSeriesList() == null) {
                var timeSeriesList = TimeSeriesList.builder()
                                                   .temporalResolution("n/a")
                                                   .unit("KW")
                                                   .permission(permission)
                                                   .build();
                newTimeSeries = getTimeSeriesFromRTDEnvelope(rtdEnvelope, timeSeriesList);
                timeSeriesList.setTimeSeries(newTimeSeries);
                permission.setTimeSeriesList(timeSeriesList);
                permissionRepository.save(permission);
                log.debug("Created new Time Series List for permission with EDDIE permission id {}", eddiePermissionId);
            } else {
                newTimeSeries = getTimeSeriesFromRTDEnvelope(rtdEnvelope, permission.getTimeSeriesList());
                var savedTimeSeries = permission.getTimeSeriesList().getTimeSeries();
                savedTimeSeries.addAll(newTimeSeries);
                var sizeBeforeRemove = savedTimeSeries.size();
                removeTimeSeriesOlderThan(savedTimeSeries, Duration.ofMinutes(exampleAppConfig.rtdLifetimeWindowMinutes()));

                permissionRepository.save(permission);
                var deletedTimeSeriesCount = sizeBeforeRemove - savedTimeSeries.size();
                log.debug("Added {} new Time Series and removed {} Time Series (older than {} minutes) for permission {} with EDDIE permission id {}",
                          newTimeSeries.size(),
                          deletedTimeSeriesCount,
                          exampleAppConfig.rtdLifetimeWindowMinutes(),
                          permission.getId(),
                          eddiePermissionId);
            }

            var websocketTopic = RTD_WEBSOCKET_TOPIC_PREFIX + permission.getId();
            if (webSocketService.topicHasActiveSession(websocketTopic)) {
                try {
                    messagingTemplate.convertAndSend(websocketTopic, objectMapper.writeValueAsString(newTimeSeries));
                    log.debug("Published new Time Series for permission with permission id {} on websocket topic!",
                              permission.getId());
                } catch (JsonProcessingException e) {
                    log.warn(
                            "Failed to published new Time Series for permission with permission id {} on websocket topic due to a serialization error!",
                            permission.getId());
                }
            }
        }, () -> {
            log.debug("Received message for EDDIE permission id {}, which is not tracked! Ignoring Message!",
                      eddiePermissionId);
        });
    }

    private List<TimeSeries> getTimeSeriesFromRTDEnvelope(RTDEnvelope rtdEnvelope, TimeSeriesList timeSeriesList) {
        var result = new ArrayList<TimeSeries>();
        for (var cimTimeSeries : rtdEnvelope.getMarketDocument().getTimeSeries()) {
            for (var quantity : cimTimeSeries.getQuantities()) {
                if (quantity.getType().equals(QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT__KW)) {
                    result.add(TimeSeries.builder()
                                         .value(quantity.getQuantity().doubleValue())
                                         .timestamp(cimTimeSeries.getDateAndOrTimeDateTime().toInstant())
                                         .timeSeriesList(timeSeriesList)
                                         .build()
                    );
                }
            }
        }
        return result;
    }

    private void removeTimeSeriesOlderThan(List<TimeSeries> timeSeries, TemporalAmount temporalAmount) {
        var oldestAllowedTimestamp = Instant.now().minus(temporalAmount);
        timeSeries.removeIf(ts -> ts.getTimestamp().isBefore(oldestAllowedTimestamp));
    }
}
