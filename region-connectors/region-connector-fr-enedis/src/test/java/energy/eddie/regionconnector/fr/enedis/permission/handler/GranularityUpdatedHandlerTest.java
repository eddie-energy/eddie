// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrGranularityUpdateEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrUsagePointTypeEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GranularityUpdatedHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private FrPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private GranularityUpdatedHandler handler;

    @Test
    void accept_onUpdatedGranularity_emitsNewUsagePointEvent() {
        // Given
        var request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setUsagePointId("usagePointId")
                .create();
        when(repository.getByPermissionId("pid")).thenReturn(request);

        // When
        eventBus.emit(new FrGranularityUpdateEvent("pid", Granularity.P1D));

        // Then
        verify(outbox).commit(assertArg(event -> assertThat(event)
                .asInstanceOf(InstanceOfAssertFactories.type(FrUsagePointTypeEvent.class))
                .extracting(FrUsagePointTypeEvent::usagePointType)
                .isEqualTo(UsagePointType.CONSUMPTION)));
    }
}