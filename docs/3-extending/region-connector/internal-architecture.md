# Internal Architecture

While each region connector could implement a completely different architecture, since they are isolated from another, it is better to use the same architecture.
That way it is possible to reuse existing shared functionality, where only one bean instance has to be created for a shared implementation in the region connector context.

[The permission process model](../../2-integrating/integrating.md#permission-process-model) has to be implemented by each region connector.
When a permission request changes status, it has to be emitted to the EP.

The region connectors are implemented using [event sourcing](https://learn.microsoft.com/en-us/azure/architecture/patterns/event-sourcing).
Event sourcing uses events to build an aggregate.
An event is a change in a certain system.
All events contain an aggregate ID, which identifies all events that are related to each other.
Using the aggregate ID, it is possible to create an aggregate.
This aggregate is the domain entity.
In this case, a permission request is built from permission events.
All permission events with the same permission ID represent the current status of the permission request.
For more detailed information on event sourcing see [this blog post](https://martinfowler.com/eaaDev/EventSourcing.html).

## Event Store

An event store is the database that persists events and allows applications to load the aggregate.
There are two ways to rebuild the domain entity:

- Load all events from the store and build the aggregate in the code
- Utilize the event stores capabilities to create the aggregate.

In EDDIE's case the second approach was chosen.
PostgreSQL is the event store.
This is done by using append-only tables, which just have read-write enabled.
The mapping from events to the event table is done by Hibernate.
An abstract [base event](#persistable-event) is defined, which contains all the necessary Hibernate configuration to persist multiple classes inheriting the base event.
All events in a region connector inherited from this base event.
The different implementations of the base event can vary, some might only contain the same information as the base event, others may add many additional fields.
For example, the [created event](#created-event) often contains a metering point ID.

> [!WARNING]
> Since an append-only table is used, don't save any credentials, such as access tokens in events.
> This should be saved in their own tabel, that allows delete operations.

The aggregate is created in PostgreSQL as a view.
That way it is possible to retrieve permission requests just as if they are persisted to a normal table via Hibernate.
No special configuration is needed for that.
PostgresQL provides [window functions](https://www.postgresql.org/docs/current/tutorial-window.html), which allows operating on related rows.
The aggregate is created by executing an aggregate functions on column of for all related rows.
In many cases, the aggregate function that's used is the `firstval_agg` function.
It gets the latest non-null value for a column.
Of course, any other aggregate function can be used if needed.
The following is an example of the event table and the permission request view that recreates the aggregate.

```sql
CREATE TABLE foo_bar.permission_event
(
    dtype varchar(31) NOT NULL,                           -- the type of event, needed by Hibernate to persist the event
    id                      bigserial   NOT NULL,         -- unique ID for just this event
    event_created           timestamp(6) WITH TIME ZONE,  -- timestamp of when the event was created
  -- The following have to be present in each permission request, but not in each event
    permission_id           varchar(36),                  -- the aggregate ID
    status                  text,                         -- the current status of the permission request
    data_start              date,
    data_end                date,
    granularity             text,
  -- here any other fields needed by events
    PRIMARY KEY (id)
);

-- Create the get latest non null value aggregate function
CREATE FUNCTION foo_bar.coalesce2(anyelement, anyelement) RETURNS anyelement
    LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE foo_bar.firstval_agg(anyelement)
    (SFUNC = foo_bar.coalesce2, STYPE =anyelement);
--

-- Create the permission request view from the events, by aggregating the fields
CREATE VIEW foo_bar.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   foo_bar.firstval_agg(connection_id) OVER w           AS connection_id,
                                   MIN(event_created) OVER w                            AS created, -- here the min aggregate function is used, to get the smallest value
                                   foo_bar.firstval_agg(data_need_id) OVER w            AS data_need_id,
                                   foo_bar.firstval_agg(granularity) OVER w             AS granularity,
                                   foo_bar.firstval_agg(permission_start) OVER w        AS permission_start,
                                   foo_bar.firstval_agg(permission_end) OVER w          AS permission_end,
                                   foo_bar.firstval_agg(status) OVER w                  AS status,
FROM foo_bar.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC) -- Order by event_created to get the newest events first
ORDER BY permission_id, event_created;
```

In order for Hibernate to pick up the view as table, the permission request implementation needs a few modifications.

```java
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import jakarta.persistence.*;

@Entity
@Table(name = "permission_request", schema = "foo_bar")
public class FluviusPermissionRequest implements PermissionRequest {
  // Omitted...
}
```

### Schema Migration

Since the integration of the permission event table and permission request view are solely done via SQL, it is necessary to provide schema migration.
This is done via [Flyway](https://github.com/flyway/flyway).
The core runs schema migrations for all region connectors.
The schemas have to be provided in the resource directory under `db/migration/<region-connector-name>/V<major-version>_<minor-version>__<name>.sql`
The migrations are automatically executed on startup.

## Event Bus

Event sourcing utilizes an event bus to send events to event handlers.
The event bus can be an external component, but it also can be provided via a library.
In EDDIE's case, Project Reactor is used to provide [the event bus](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/regionconnector/shared/event/sourcing/EventBus.html).
The event bus is an internal component.
Event handlers can subscribe to specific events via the event bus.
There are two kinds of [event handlers](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/regionconnector/shared/event/sourcing/handlers/EventHandler.html).

The integration event handler is used to integrate the events to external systems such as Kafka.
There are already two implementations for [connection status messages](./shared-functionality.md#connectionstatusmessagehandler) and [permission market documents](./shared-functionality.md#permissionmarketdocumentmessagehandler).
They integrate events to the outbound connectors.

The second kind is the domain event handler.
Domain event handlers react to event and execute business logic.
For example, when a validated event is emitted, a domain event handler can subscribe to those and send the permission request to the PA.

## Outbox

While it is possible to implement an integration event handler to persist events, this approach has been shown to be brittle in combination with Hibernate, since it could be that events are emitted faster than they are persisted.
Instead [the outbox pattern](https://microservices.io/patterns/data/transactional-outbox.html) has been chosen.
The outbox first persists the permission event and then emits the event to the event bus.
That way only persisted events are ever sent to the event bus.
The implementation can be found [here](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/regionconnector/shared/event/sourcing/Outbox.html).

## Implementing the Events

For each permission process the mapping to the permission process model can be different.
Some permission processes of PAs assume more interaction with the EP than others.
The mapping has to be done on a case per case basis.
The following should give a rough unterstanding on how to map the different statuses to the permission process of the PA.
Furthermore, some implementation details are given on how to implement the events.

<!-- @include: ../../parts/permission-process-model.md -->

### Persistable Event

The root of all events of a region connector is going to be the persistable event.
The following is an example of a peristable event.

```java
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "FoobarPersistablePermissionEvent")
// Inheritance type is important in order for the view of the permission request to work
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
// Point Hibernate to the correct schema
@Table(schema = "foo_bar", name = "permission_event")
// Supress warnings for all entities, since Hibernate does not play nice with non-null fields.
@SuppressWarnings({"NullAway", "unused"})
public abstract class PersistablePermissionEvent implements PermissionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    // Aggregate ID
    @Column(length = 36, name = "permission_id")
    // permissionIDs are usually UUIDs, but that depends on the implementation of the region connector
    private final String permissionId;
    private final ZonedDateTime eventCreated;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    // Persist enums as text instead of as numbers to make debugging and integration with other systems easier
    private final PermissionProcessStatus status;

    protected PersistablePermissionEvent(
            String permissionId,
            PermissionProcessStatus status,
            ZonedDateTime created
    ) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = created;
        this.status = status;
    }

    // Required by Hibernate
    protected PersistablePermissionEvent() {
        this.id = null;
        permissionId = null;
        eventCreated = null;
        status = null;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public ZonedDateTime eventCreated() {
        return eventCreated;
    }
}
```

While the permission request view has to be created by hand, it is possible to let Hibernate generate the DDL for the permission event table.
See [Spring docs](https://docs.spring.io/spring-boot/docs/1.1.0.M1/reference/html/howto-database-initialization.html) on how to let Hibernate generate the DDL.
In order to see the SQL printed to the console set the following property: `spring.jpa.show-sql=true`.

> [!INFO]
> Enable just your region connector, otherwise Hibernate will fail during startup, because it will try to generate the schema for other region connectors as well.

Here a full example that can be pasted in the application.properties file:

```properties
spring.jpa.show-sql=true
spring.jpa.generate-ddl=true
```

### Created Event

Once a permission request needs to be created the data is sent to the REST endpoint for creation of permission requests.
For more info on the REST endpoints see [dispatcher servlet](./dispatcher-servlet.md).
Once the data is received a created event has to be emitted via the outbox containing all the data that does not need any validation, such as connection ID, permission ID, which is created by EDDIE, data need ID, etc.

```java
@Entity(name = "CreatedEvent")
public class CreatedEvent extends PersistablePermissionEvent {
    @Column(length = 36)
    private final String dataNeedId;
    @Column(columnDefinition = "text")
    private final String connectionId;

    public CreatedEvent(String permissionId, String dataNeedId, String connectionId) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.dataNeedId = dataNeedId;
        this.connectionId = connectionId;
    }

    protected CreatedEvent() {
        this.dataNeedId = null;
        this.connectionId = null;
    }
}
```

Afterwards the permission request can be validated.
The data need has to be checked for the following:

- Does it exist
- Can the region connector support this data need

This can be done using the [DataNeedCalculationService](./shared-functionality.md#dataneedcalculationserviceimpl).
The other values specific for the region connector have to be validated as well.
Creation and validation is usually done synchronously to allow for immediate feedback to the EDDIE button via REST response.

### Validated Event

Once the validation is finished successfully, the validated event is emitted.
It should contain all the fields that needed to be validated.
After the validation, the other operations can be async done via the event bus and event handlers, since the sending the permission request to the PA might take some time, as well as the PA's response.

After the permission request is validated, it has to be sent to the PA.
For OAuth, the redirect would be the sending of the permission request, but it is impossible to know if the redirect was successful.
Therefore, the [sent to PA event](#sent-to-pa-event) is emitted after the PA redirected back to EDDIE.

### Malformed Event

If the validations fail, a malformed event has to be emitted.
The validation errors should be included as an [`AttributeError`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/process/model/validation/AttributeError.html).
This contains the erroneous field and the error message.
To be able to persist a list of `AttributeErrors`, the [`AttributeErrorListConverter`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/regionconnector/shared/event/sourcing/converters/AttributeErrorListConverter.html) can be used.
This converter converts the list into a JSON structure.
Of course, other converters can be used, or even a dedicated table referenced by the malformed event.

```java
    @Convert(converter = AttributeErrorListConverter.class)
    @Column(name = "errors", columnDefinition = "text")
    private final List<AttributeError> errors;
```

The malformed state is final, and there should be no other state coming afterward.

### Sent to PA Event

Once the permission request was sent successful to the PA this event can be emitted.

### Unable to Send Event

If sending the event was not successful, this event is emitted.
This is a retry loop, so it is possible to periodically re-emit the validated event, which should trigger the sending process.
For OAuth flows, this event is unnecessary, since it is possible to redirect a final customer again.

### Timed Out Event

If the PA never sends an answer regarding the acceptance or rejection of the permission request, this event is emitted.
There is a [default implementation](./shared-functionality.md#commontimeoutservice) that checks periodically for stale permission requests and times them out.
This is a final state, which cannot be recovered.

### Invalid Event

PAs will validate the permission requests themselves again.
If the permission request is not valid, they respond with some kind of validation error.
In this case the permission request is invalid and the invalid event is emitted.
This is a final state, which cannot be recovered.

### Rejected Event

If the final customer rejects a permission request, the PA will reject the permission request.
In this case the rejected event is emitted.
This is a final state, which cannot be recovered.

### Accepted Event

If the final customer approves the permission request, the accepted event is emitted.
After this event data can be polled from the MDA.

### Unfulfillable Event

If after acceptance it turns out the data received from the MDA does not fit the data need, an unfulfillable event is emitted.
If the PA supports termination, the permission request should be externally terminated, by emitting the [requires external termination event](#requires-external-termination-event).

### Fulfilled

This event is emitted once all data is received from the MDA.
Can be a final event, but if the PA supports external termination, the [requires external termination event](#requires-external-termination-event) needs to be emitted.

### Terminated

If the permission request was accepted, the EP can decide to terminate the permission request anytime for any reason.
This is done by using a [termination document](../../2-integrating/messages/permission-market-documents.md#termination-documents)
The termination document is received by the [RegionConnector implementation](./api.md#regionconnector).
It can be a final event, but if the PA supports external termination, the [external termination event](#requires-external-termination-event) needs to be emitted.

### Requires External Termination Event

This event indicates that a permission request should be terminated with the PA.
It is an optional event, since not all PAs support terminations.
After receiving this event, the termination needs to be sent to the PA.
If the termination is successful, the [externally terminated event](#externally-terminated-event) is emitted, otherwise the [failed to terminate event](#failed-to-terminate-event) is emitted.

### Externally Terminated Event

This event indicates that the permission request was terminated with the PA.
This is a final state, which cannot be recovered.

### Failed to Terminate Event

This event is similar to the [unable to send event](#unable-to-send-event) in that sense that it allows to trigger the external termination again in case it was not possible the first time.
This is done by emitting the requires external termination event.

### Internal Events

For events that should not be propagated to outbound connectors the [`InternalPermissionEvent`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/process/model/events/InternalPermissionEvent.html) marker interface can be used in addition.
This events will be persisted and sent to the event bus, but integration event handlers have to ignore them.
Use them to if you want to send an event that does not change the status of a permission request.
For example, to trigger periodical polling.
