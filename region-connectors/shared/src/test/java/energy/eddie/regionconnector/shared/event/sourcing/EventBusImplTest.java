// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

@SuppressWarnings("resource")
class EventBusImplTest {

    @Test
    void testEmit() {
        // given
        EventBusImpl eventBus = new EventBusImpl();
        PermissionEvent event = new TestEvent("pid", PermissionProcessStatus.CREATED);

        // when
        StepVerifier.Step<PermissionEvent> step = StepVerifier.create(eventBus.filteredFlux(PermissionEvent.class))
                .then(() -> {
                          eventBus.emit(event);
                          eventBus.close();
                      }
                );

        // then
        step
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void testFluxWithClassFilter() {
        // given
        EventBusImpl eventBus = new EventBusImpl();
        TestEvent event = new TestEvent("pid", PermissionProcessStatus.CREATED);
        PermissionEvent otherEvent = mock(PermissionEvent.class);

        // when
        StepVerifier.Step<TestEvent> step = StepVerifier.create(eventBus.filteredFlux(TestEvent.class))
                .then(() -> {
                          eventBus.emit(event);
                          eventBus.emit(otherEvent);
                          eventBus.close();
                      }
                );

        // then
        step
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void testFluxWithStatusFilter() {
        // given
        EventBusImpl eventBus = new EventBusImpl();
        TestEvent created = new TestEvent("pid", PermissionProcessStatus.CREATED);
        TestEvent validated = new TestEvent("pid", PermissionProcessStatus.VALIDATED);


        // when
        StepVerifier.Step<PermissionEvent> step = StepVerifier.create(
                        eventBus.filteredFlux(PermissionProcessStatus.CREATED))
                .then(() -> {
                          eventBus.emit(created);
                          eventBus.emit(validated);
                          eventBus.close();
                      }
                );

        // then
        step
                .expectNext(created)
                .verifyComplete();
    }
}