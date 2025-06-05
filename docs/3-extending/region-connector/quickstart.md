# Quickstart

This is a simple quickstart guide to implement a new region connector.
In the following the region connector is called `foo-bar`.
The quickstart will follow this checklist:

<!-- @include: ../../parts/region-connector-checklist.md -->

For this quickstart, we are assuming a pull approach, where data is pulled from the PA and MDA.
The implementation for a push approach is just a bit different.
Events are emitted once data is pushed to the region connector, instead of reacting to responses from the API of the PA and MDA.

## Setup

First a new Gradle subproject has to be created in the [`region-connectors`](https://github.com/eddie-energy/eddie/tree/main/region-connectors) directory with the name
`region-connector-foo-bar`.
The new subproject has to be included in the [`settings.gradle.kts`](https://github.com/eddie-energy/eddie/blob/main/settings.gradle.kts) and [the core
`build.gradle.kts`](https://github.com/eddie-energy/eddie/blob/main/core/build.gradle.kts).

```kotlin
// settings.gradle.kts
include("region-connectors:region-connector-foo-bar")
findProject(":region-connectors:region-connector-foo-bar")?.name = "region-connector-foo-bar"

// core/build.gradle.kts
dependencies {
// ...
  implementation(project(":region-connectors:region-connector-foo-bar"))
//...
}
```

The new region connector requires some dependencies, so the
`build.gradle.kts` of the region connector should look something like this:

```kotlin
plugins {
  id("energy.eddie.java-conventions")
  id("energy.eddie.pnpm-build")

  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie.regionconnector.foo.bar"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  //  defined in gradle/libs.versions.toml
  implementation(project(":api"))
  implementation(project(":data-needs"))
  implementation(project(":region-connectors:shared"))
  implementation(libs.spring.boot.starter.web)

  implementation(libs.nimbus.oidc)

  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.reactor.test)
}

// disable bootJar task as it needs a main class and region connectors do not have one
tasks.getByName<BootJar>("bootJar") {
  enabled = false
}

tasks.getByName<Jar>("jar") {
  enabled = true
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  finalizedBy(tasks.jacocoTestCoverageVerification)
}

configureJavaCompileWithErrorProne("energy.eddie.foo.bar")
```

For more details on the setup see the [build and setup section](./build-and-setup.md).

## Creating a permission request

::: details Checklist Status

- :arrow_right: Create a permission request at the permission administrators side
- Implement custom element for region connector
- Implement permission market documents
- Request validated historical data and emit it to raw data stream
- Map validated historical data to validated historical data market documents
- Allow data needs for future data, and request the data once available
- Request accounting point data and emit it to raw data stream
- Map accounting point data to accounting point market document
- Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

First, we have to create a permission request on EDDIE's side via the region connector.
A region connector is necessary to create, validate, and manage permission requests on the EP's side.
So the first thing to do is create the permission request internally.

### Structure

To do that, the Spring application has to be initialized and marked as a region connector.
This is everything that is needed to set up the first part of the region connector.
For more details, see [API](./api.md).

```java
package energy.eddie.regionconnector.foo.bar;

import energy.eddie.api.agnostic.RegionConnector;

@SpringBootApplication
@RegionConnector(name = "foo-bar")
public class FooBarSpringConfig {
}
```

Next the metadata and the region connector itself have to be implemented.
The metadata is necessary for the core to root between the different region connectors, as well as supplying crucial information regarding the PA and MDA.

```java

package energy.eddie.regionconnector.foo.bar;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class FooBarRegionConnector implements RegionConnector {

  @Override
  public RegionConnectorMetadata getMetadata() {
    return FooBarRegionConnectorMetadata.getInstance();
  }

  @Override
  public void terminatePermission(String permissionId) {
    // TODO: Will be implemented later
  }
}
```

And the metadata.

```java
package energy.eddie.regionconnector.foo.bar;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;

import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;

public class FooBarRegionConnectorMetadata implements RegionConnectorMetadata {
  public static final String REGION_CONNECTOR_ID = "foo-bar";
  public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of();
  private static final FluviusRegionConnectorMetadata INSTANCE = new FluviusRegionConnectorMetadata();

  private FluviusRegionConnectorMetadata() {}

  public static RegionConnectorMetadata getInstance() {
    return INSTANCE;
  }

  @Override
  public String id() {
    return REGION_CONNECTOR_ID;
  }

  @Override
  public String countryCode() {
    return "foo";
  }

  @Override
  public long coveredMeteringPoints() {
    // TODO: find out for your region and MDA
    return 0;
  }

  @Override
  public Period earliestStart() {
    // TODO: find out for your region, what the oldest data is that you can request
    return Period.ofYears(0);
  }

  @Override
  public Period latestEnd() {
    // TODO: find out for your region, what the newest data is that you can request
    return Period.ofYears(0);
  }

  @Override
  public List<Granularity> supportedGranularities() {
    // TODO: find out what the resolutions of the data are that your MDA supports
    return List.of();
  }

  @Override
  public ZoneId timeZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public List<Class<? extends DataNeedInterface>> supportedDataNeeds() {
    return List.copyOf(SUPPORTED_DATA_NEEDS);
  }
}
```

With the region connector metadata in place go ahead and update the `FooBarSpringConfig`.

```java

@SpringBootApplication
@RegionConnector(name = FooBarRegionConnectorMetadata.REGION_CONNECTOR_ID)
public class FooBarSpringConfig {
}
```

Now all the essential parts are implemented, so let's start with implementing the creation of the permission request.

### Implementing the permission event

Region connectors use an event sourcing approach internally, where a permission request is an aggregate of multiple related permission events.
The events are stored in one single table, and each event class inherits from one base event called the
`PersistablePermissionEvent`.
The
`FooBar` prefix is dropped here for readability, but required in the entity name, since other region connectors define similarly named base classes, which leads to name clashes.
More information can be found in [the internal architecture section](./internal-architecture.md).

```java
package energy.eddie.regionconnector.foo.bar.permission.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "FooBarPersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = "foo_bar", name = "permission_event")
public abstract class PersistablePermissionEvent implements PermissionEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private final Long id;
  // Aggregate ID
  @Column(length = 36)
  private final String permissionId;
  private final ZonedDateTime eventCreated;
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "text")
  private final PermissionProcessStatus status;

  protected PersistablePermissionEvent(
          String permissionId,
          PermissionProcessStatus status
  ) {
    this.id = null;
    this.permissionId = permissionId;
    this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
    this.status = status;
  }

  protected PersistablePermissionEvent() {
    this.id = null;
    permissionId = null;
    eventCreated = null;
    status = null;
  }
  // Overrides...
}
```

Each following event in this region connector has to implement the `PersistablePermissionEvent`.
To persist it in the event store, the table has to be created and extended with additional columns depending on what the permission request looks like.

> [!IMPORTANT]
> Put the SQL statements into an SQL script in the resource folder as described in [schema migration](./internal-architecture.md#schema-migration).

```sql
CREATE TABLE foo_bar.permission_event
(
  dtype         varchar(31) NOT NULL,
  id            bigserial   NOT NULL,
  event_created timestamp(6) WITH TIME ZONE,
  permission_id varchar(36) NOT NULL,
  connection_id text,
  data_need_id  varchar(36),
  status        text        NOT NULL,
  data_start    date,
  data_end      date,
  granularity   text,
  PRIMARY KEY (id)
);
```

Persistence is done using Spring repositories.
The repository for permission events is usually only needed for reading and writing, since update and delete should be disabled for the permission event table.

```java
package energy.eddie.regionconnector.foo.bar.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.foo.bar.permission.events.PersistablePermissionEvent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FooBarPermissionEventRepository extends PermissionEventRepository, CrudRepository<PersistablePermissionEvent, Long> {
}
```

This is all that's needed to create permission events.

### Creating the permission request class

To create a permission request, the first thing that's needed is the permission request itself.
For more information regarding the permission request and its persistence, see [the internal architecture](./internal-architecture.md#event-store).

```java
package energy.eddie.regionconnector.foo.bar.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "permission_request", schema = "foo_bar")
public class FooBarPermissionRequest implements PermissionRequest {
  @Id
  @Column(name = "permission_id")
  private final String permissionId;
  @Column(name = "connection_id")
  private final String connectionId;
  @Column(name = "data_need_id")
  private final String dataNeedId;
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private final PermissionProcessStatus status;
  @Column(name = "granularity")
  @Enumerated(EnumType.STRING)
  private final Granularity granularity;
  @Column(name = "data_start")
  private final LocalDate start;
  @Column(name = "data_end")
  private final LocalDate end;
  @Column(name = "created")
  private final ZonedDateTime created;
  // Additional attributes here

  // no args constructor for hibernate

  @Override
  public DataSourceInformation dataSourceInformation() {
    return new FooBarDataSourceInformation();
  }
  // Overrides here
}
```

The permission request requires a class called `DataSourceInformation`, which carries information regarding PA and MDA.
This is often just a simple POJO with static attributes, since a lot of times there is only one PA and MDA.

```java
package energy.eddie.regionconnector.foo.bar.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.regionconnector.foo.bar.FooBarRegionConnectorMetadata;

public class FooBarDataSourceInformation implements DataSourceInformation {

  @Override
  public String countryCode() {
    return "Foo";
  }

  @Override
  public String regionConnectorId() {
    return FooBarRegionConnectorMetadata.REGION_CONNECTOR_ID;
  }

  @Override
  public String meteredDataAdministratorId() {
    return "foo-bar";
  }

  @Override
  public String permissionAdministratorId() {
    return "foo-bar";
  }
}
```

Once the permission request itself is implemented, it has to be read from the database.
There is no need to persist it, since it will be recreated from an event table, which is done later in this quickstart.

```java
package energy.eddie.regionconnector.foo.bar.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.regionconnector.foo.bar.permission.request.FooBarPermissionRequest;

@org.springframework.stereotype.Repository
public interface FooBarPermissionRequestRepository
        extends PermissionRequestRepository<FooBarPermissionRequest>,
        Repository<FooBarPermissionRequest, String> {
}
```

Since the permission requests are just aggregates of permission events, they have to be recreated somewhere.
This is done via a view in PostgreSQL.

```sql
CREATE FUNCTION foo_bar.coalesce2(anyelement, anyelement) RETURNS anyelement
  LANGUAGE sql AS
'SELECT COALESCE($1, $2)';

CREATE AGGREGATE foo_bar.firstval_agg(anyelement)
  (SFUNC = foo_bar.coalesce2, STYPE =anyelement);

CREATE VIEW foo_bar.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   foo_bar.firstval_agg(connection_id) OVER w AS connection_id,
                                   foo_bar.firstval_agg(data_need_id) OVER w  AS data_need_id,
                                   foo_bar.firstval_agg(status) OVER w        AS status,
                                   foo_bar.firstval_agg(data_start) OVER w    AS data_start,
                                   foo_bar.firstval_agg(data_end) OVER w      AS data_end,
                                   foo_bar.firstval_agg(granularity) OVER w   AS granularity,
                                   MIN(event_created) OVER w                  AS created
FROM foo_bar.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
```

### Creating the permission request via REST calls

Now, the permission request and repositories are implemented the next part is to create the REST endpoints to create a permission request.
First enable WebMvc for this region connector and create instances of the event bus and outbox.

```java
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;

@SpringBootApplication
@RegionConnector(name = FooBarRegionConnectorMetadata.REGION_CONNECTOR_ID)
@EnableWebMvc
public class FooBarSpringConfig {
  @Bean
  public EventBus eventbus() {
    return new EventBusImpl();
  }

  @Bean
  public Outbox outbox(EventBus eventbus, FooBarPermissionEventRepository repo) {
    return new Outbox(eventbus, repo);
  }
}
```

Since we want to create a permission request, we need to emit a created event.
Which indicates that a permission request was created and contains all the data sent by the final customer.

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

  // No args constructor 
}
```

Next, a rest controller can be created, which gets the data from the EDDIE button and creates the permission request.

```java

@RestController
public class PermissionRequestController {
  private final Outbox outbox;

  public PermissionRequestController(Outbox outbox) {
    this.outbox = outbox;
  }

  @PostMapping(
          value = PATH_PERMISSION_REQUEST,
          produces = MediaType.APPLICATION_JSON_VALUE,
          consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@Valid @RequestBody PermissionRequestForCreation dto) throws DataNeedNotFoundException, UnsupportedDataNeedException {
    var permissionId = UUID.randomUUID().toString();
    outbox.commit(new CreatedEvent(permissionId, dto.dataNeedId(), dto.connectionId()));
    // TODO: Validation
    var pr = new CreatedPermissionRequest(permissionId);
    return ResponseEntity.created(new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId))
                         .body(pr);
  }

  public record CreatedPermissionRequest(String permissionId) {}

  public record PermissionRequestForCreation(String dataNeedId,
                                             String connectionId /* Information needed from the final customer */) {}
}
```

Now that the permission request is created, it has to be validated too.
Create a validation event and a malformed event.
The malformed event should include all the validation errors.
For the mapping see [malformed event](./internal-architecture.md#malformed-event).

```java

@Entity
public class ValidatedEvent extends PersistablePermissionEvent {
  @Column(name = "data_start")
  private final LocalDate DataStart;
  @Column(name = "end_start")
  private final LocalDate DataEnd;
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "text")
  private final AllowedGranularity granularity;
}

@Entity
public class MalformedEvent extends PersistablePermissionEvent {
  @Convert(converter = AttributeErrorListConverter.class)
  @Column(name = "errors", columnDefinition = "text")
  private final List<AttributeError> errors;

  public MalformedEvent(String permissionId, List<AttributeError> errors) {
    super(permissionId, PermissionProcessStatus.MALFORMED);
    this.errors = errors;
  }

  public MalformedEvent(String permissionId, AttributeError error) {
    super(permissionId, PermissionProcessStatus.MALFORMED);
    this.errors = List.of(error);
  }
}
```

To validate the data need ID an instance of the [`DataNeedCalculationService`](./api.md#dataneedcalculationservice) is needed.
Create a bean of this type in your `FooBarSpringConfig` class.

```java

@Bean
public DataNeedCalculationService calcService(DataNeedsService dataNeedsService) {
  return new DataNeedCalculationServiceImpl(dataNeedsService, FooBarRegionConnectorMetadata.getInstance());
}
```

Extend the REST controller to include validation.
In this code example, the REST controller validates the permission request, polls the data need, and depending on the result, sends a validated event or malformed event.
That is everything needed to create and validate a permission request on EDDIE's side.

```java

@RestController
public class PermissionRequestController {
  private final DataNeedCalculationService calculationService;

  @PostMapping(
          value = PATH_PERMISSION_REQUEST,
          produces = MediaType.APPLICATION_JSON_VALUE,
          consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@Valid @RequestBody PermissionRequestForCreation dto) throws DataNeedNotFoundException, UnsupportedDataNeedException {
    var permissionId = UUID.randomUUID().toString();
    outbox.commit(new CreatedEvent(permissionId, dto.dataNeedId(), dto.connectionId()));
    // new code start
    if (!isValid(dto)) {
      outbox.commit(new MalformedEvent(permissionId, /* TODO: Include the validation errors here */));
      return ResponseEntity.badRequest().body("Invalid request body");
    }
    var calculation = calculationService.calculate(dto.dataNeedId());
    switch (calculation) {
      case DataNeedNotFoundResult ignored -> {
        outbox.commit(new MalformedEvent(permissionId,
                                         new AttributeError("dataNeedId", "data need not found")));
        throw new DataNeedNotFoundException(dto.dataNeedId());
      }
      case DataNeedNotSupportedResult ignored -> {
        outbox.commit(new MalformedEvent(permissionId,
                                         new AttributeError("dataNeedId", "data need not supported")));
        throw new DataNeedNotSupportedException(REGION_CONNECTOR_ID,
                                                dto.dataNeedId(),
                                                "data need not supported");
      }
      case ValidatedHistoricalDataDataNeedResult result -> outbox.commit(new ValidatedEvent(permissionId,
                                                                                            result.energyTimeframe.start(),
                                                                                            result.energyTimeframe.end(),
                                                                                            result.energyTimeframe.granularities()
                                                                                                                  .getFirst()));
      case AccountingPointDataNeedResult ignored -> outbox.commit(new ValidatedEvent(permissionId, null, null, null));
    }
    // new code end
    var pr = new CreatedPermissionRequest(permissionId);
    return ResponseEntity.created(new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId))
                         .body(pr);
  }

  private boolean isValid(PermissionRequestForCreation dto) {
    // TODO: validate
  }
}
```

### Send the permission request to the PA

After the permission request was validated, it can be sent to the permission administrator.

> [!TIP]
> If the PA you're implementing does employ an OAUTH flow or something similar, this part is not relevant.
> In this case, emit a sent to PA event once the final customer is redirected back to your region connector.

This time we can use the event sourcing approach by implementing the sending functionality via a [domain event handler](./internal-architecture.md#event-bus).
Once the validation event is emitted via the eventbus, we can subscribe to these kinds of events and process them further.
We implement a validation event handler that sends all validated permission requests to the PA.
We then await the response from the API client, which we assume uses Project Reactor, but this is up to you.
If the request was successful, a sent to PA event is emitted, otherwise a unable to send is emitted to the event bus.

```java

@Component
public class ValidatedEventHandler implements EventHandler<ValidatedEvent> {
  private final FooBarPermissionRequestRepository repo;
  private final Outbox outbox;
  // TODO: Implement your own api client for your PA
  private final ApiClient api;

  public ValidatedEventHandler(
          FooBarPermissionRequestRepository repo,
          Outbox outbox,
          ApiClient api,
          EventBus eventBus
  ) {
    this.repo = repo;
    this.api = api;
    eventBus.filteredFlux(ValidatedEvent.class)
            .subscribe(this::accept);
  }

  @Override
  public void accept(ValidatedEvent event) {
    var pr = repo.getByPermissionId(event.permissionId());
    api.createPermissionRequest(pr)
       .subscribe(resp -> {
         if (resp.isSuccessResponse()) {
           outbox.commit(new SentToPaEvent(event.permissionId()));
         } else {
           outbox.commit(new UnableToSendEvent(event.permissionId()));
         }
       });
  }
}
```

Now everything is implemented to create permission request on the PA's side too, but since we could run into errors when sending the request, we can implement a retry too.
A new validated event implementation is used, since the old one described in the [section above](#creating-the-permission-request-via-rest-calls) would require the start, end, and granularity again, which we already know and didn't change.
This service searches for permission requests with the unable to send status every hour, or using the value provided by the configuration property.

```java

@Service
public class RetryService {
  private final FooBarPermissionRequestRepository repo;
  private final Outbox outbox;

  public RetryService(FooBarPermissionRequestrepo repo, Outbox outbox) {
    this.repo = repo;
    this.outbox = outbox;
  }

  @Scheduled(cron = "${region-connector.foo.bar.retry:0 0 * * * *}")
  public void retry() {
    var permissionRequests = repo.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND);
    for (var permissionRequest : permissionRequests) {
      outbox.commit(new RetryValidatedEvent(permissionRequest.permissionId()));
    }
  }
}
```

To be able to search by status the permission request repository has to be updated.

```java
package energy.eddie.regionconnector.foo.bar.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.regionconnector.foo.bar.permission.request.FooBarPermissionRequest;

@org.springframework.stereotype.Repository
public interface FooBarPermissionRequestRepository
        extends PermissionRequestRepository<FooBarPermissionRequest>,
        StatusPermissionRequestRepository<FooBarPermissionRequest>,
        Repository<FooBarPermissionRequest, String> {
}
```

### Acceptance and Rejection

The permission request is sent to the PA, who will let us now via their chosen channel if a permission request was accepted or rejected.
We assume for this example that the PA will redirect the final customers to a callback URI hosted by our region connector.
So we have to extend the REST controller.
Instead of just supporting REST, the controller will respond with a rendered Thymeleaf HTML page.
So go ahead and add the spring Thymeleaf starter to your `build.gradle.kts`.

```kotlin
dependencies {
  // other dependencies...
  implementation(libs.spring.boot.starter.thymeleaf)
}
```

There exists a [Thymeleaf template](./shared-functionality.md#authorization-callback-template), that gives feedback about a permission request to the final customer.
We are going to use that to inform them what happened to their permission request.

```java

@Controller
public class PermissionRequestController {
  private final Outbox outbox;
  private final FooBarPermissionRequestRepository repo;

  @GetMapping("permission-request/callback")
  public String callback(
          @RequestParam(value = "error", required = false) String error,
          @RequestParam(value = "state") String permissionId,
          Model model
  ) {
    var pr = repo.findByPermissionId(permissionId);
    String status;
    if (pr.isEmpty()) {
      status = "ERROR";
      return "authorization-callback";
    }

    if (error == null) {
      outbox.commit(new AcceptedEvent(permissionId));
      status = "OK";
    } else if (error.equals("rejected")) {
      outbox.commit(new RejectedEvent(permissionId));
      status = "DENIED";
    } else {
      outbox.commit(new InvalidEvent(permissionId));
      status = "ERROR";
    }
    model.addAttribute("status", status);
    var dnid = pr.get().dataNeedId();
    model.addAttribute("dataNeedId", dnid);
    return "authorization-callback";
  }

  // other methods...
}
```

## Implementing the frontend

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :arrow_right: Implement custom element for region connector
- Implement permission market documents
- Request validated historical data and emit it to raw data stream
- Map validated historical data to validated historical data market documents
- Allow data needs for future data, and request the data once available
- Request accounting point data and emit it to raw data stream
- Map accounting point data to accounting point market document
- Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

Now that we can create permission requests via the REST API provided by the region connector, we have to connect it to the EDDIE button.
First initialize a new pnpm module in the same directory as the region connector subproject.

## Enable Permission Market Documents and Connection Status Messages

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :arrow_right: Implement permission market documents
- Request validated historical data and emit it to raw data stream
- Map validated historical data to validated historical data market documents
- Allow data needs for future data, and request the data once available
- Request accounting point data and emit it to raw data stream
- Map accounting point data to accounting point market document
- Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

It is rather easy to enable [permission market documents](./shared-functionality.md#permissionmarketdocumentmessagehandler) and [connections status messages](./shared-functionality.md#connectionstatusmessagehandler), since there are already implementations that work across the region connectors.
The implementations just have to be defined as Spring beans in your Spring config.

```java

@SpringBootApplication
@RegionConnector(name = "foo-bar")
public class FooBarSpringConfig {

  // For connection status messages
  @Bean
  public ConnectionStatusMessageHandler<FooBarPermissionRequest> connectionStatusMessageHandler(
          EventBus eventBus,
          FooBarPermissionRequestRepository repository
  ) {
    return new ConnectionStatusMessageHandler<>(
            eventBus,
            repository,
            pr -> ""
    );
  }

  // For permission market documents, the CIM pendant to connection status messages
  @Bean
  public PermissionMarketDocumentMessageHandler<FooBarPermissionRequest> permissionMarketDocumentMessageHandler(
          EventBus eventBus,
          FooBarPermissionRequestRepository repo,
          DataNeedsService dataNeedsService,
          CommonInformationModelConfiguration cimConfig
  ) {
    return new PermissionMarketDocumentMessageHandler<>(
            eventBus,
            repo,
            dataNeedsService,
            cimConfig.eligiblePartyFallbackId(),
            cimConfig,
            pr -> null,
            ZoneOffset.UTC
    );
  }
}
```

These implementations register themselves as a catch-all event handler in the event bus.
When they receive a new event, they generate a new connection status message or permission market document.

## Request Validated Historical Data

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :arrow_right: Request validated historical data and emit it to raw data stream
- Map validated historical data to validated historical data market documents
- Allow data needs for future data, and request the data once available
- Request accounting point data and emit it to raw data stream
- Map accounting point data to accounting point market document
- Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :arrow_right: React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

Now we want to support historical validated data, so we have to update the supported data needs list in the region connector metadata implementation.
Validated historical data is a term that describes metering data that was validated by the MDA.
We take that validated historical data in the MDA's format and want to publish it.
First, we are going to emit it as raw data, so we take the validated historical data as is and emit it to the outbound connectors.
Later on, we will map the validated historical data to the validated historical data market documents, which is a CIM format.

```java
public class FooBarRegionConnectorMetadata implements RegionConnectorMetadata {
  public static final List<Class<? extends DataNeed>> SUPPORTED_DATA_NEEDS = List.of(
          ValidatedHistoricalDataDataNeed.class
  );
}
```

When a permission request is created now, it won't be malformed anymore, since one data need is supported now.
Next, the validated historical data has to be requested and published to the raw data stream.
Since the same data is going to be emitted as a validated historical data market document later on, there should be one base stream that gets converted into the necessary format.

```java

@Component
public class ValidatedHistoricalDataStream {
  private final Sink<IdentifiableValidatedHistoricalData> sink = Sinks.many().multicast().onBackpressureBuffer();

  public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
    return sink.asFlux();
  }

  public void publish(FooBarPermissionRequest pr, Data data) {
    sink.tryEmitNext(new IdentifiableValidatedHistoricalData(pr, data));
  }
}
```

The `IdentifiableValidatedHistoricalData` class is used to identify a specific API response with a permission request.
It is used to convert the same API response to raw data messages and validated historical data market documents, which will be done later.

```java
public record IdentifiableValidatedHistoricalData(
        FooBarPermissionRequest permissionRequest,
        MeteredData payload
) implements IdentifiablePayload<FooBarPermissionRequest, MeteredData> {}
```

The infrastructure to publish the messages in different formats is in place.
The next step is to actually request the validated historical data from your MDA.
For that, extend your API client or similar to allow requesting this data.
Since only validated historical data from the past is supported, permission requests for the future are ignored for now.
Once a permission request is accepted, data can be requested.
This functionality is implemented as domain event handler again.
The event handler subscribes to all `AcceptedEvent`s and requests the data for each permission request.
The data is published to the `ValidatedHistoricalDataStream`.
If the request resulted in an exception, it is handled differently.
If the exception indicates that the final customer revoked the permission, a `RevokedEvent` is emitted.

```java

@Component
public class AcceptedHandler implements EventHandler<PermissionEvent> {
  private final FooBarPermissionRequestRepository repo;
  // TODO: Implement your own api client
  private final ApiClient api;
  private final ValidatedHistoricalDataStream stream;
  private final Outbox outbox;

  public AcceptedHandler(
          EventBus eventBus,
          FooBarPermissionRequestRepository repo,
          ApiClient api,
          ValidatedHistoricalDataStream stream,
          Outbox outbox
  ) {
    this.repo = repo;
    this.api = api;
    this.stream = stream;
    this.outbox = outbox;
    eventBus.filteredFlux(AcceptedEvent.class)
            .subscribe(this::accept);
  }

  public void accept(PermissionEvent event) {
    var pr = repo.getByPermissionId(event.permissionId());
    api.meteredData(pr)
       .subscribe(
               result -> stream.publish(pr, result),
               error -> handleError(error, event.permissionId())
       );
  }

  private void handleError(Throwable error, String permissionId) {
    // Example from the Spring reactive webclient
    // Forbidden usually indicates that the final customer revoked the permission
    if (error instanceof HttpClientErrorException.Forbidden) {
      outbox.commit(new RevokedEvent(permissionId));
    } else {
      // TODO: handle error any other way
    }
  }
}
```

Since we already have the information if a permission request was fulfilled or what the latest meter reading was for a permission request, let's implement fulfillment.
If you implement the [`MeterReadingPermissionRequest`](./api.md#meterreadingpermissionrequest) you can use the [`MeterReadingUpdateAndFulfillmentService`](./shared-functionality.md#meterreadingpermissionupdateandfulfillmentservice).
It provides a default implementation that updates a permission request with the latest meter reading and fulfills it too if all data was requested.

```java

@Service
public class MeterReadingUpdateService {
  private final Outbox outbox;

  public MeterReadingUpdateService(ValidatedHistoricalDataStream stream, Outbox outbox) {
    this.outbox = outbox;
    stream.validatedHistoricalData()
          .subscribe(this::handleMeterReading);
  }

  public void handleMeterReading(IdentifiableValidatedHistoricalData data) {
    ZonedDateTime latestReading = // TODO: get latest reading date time from data.payload()
            outbox.commit(new LatestMeterReadingEvent(data.permissionRequest().permissionId(), latestReading));
  }
}
```

A special type of event is used to update the latest meter reading, the [`InternalPermissionEvent`](./internal-architecture.md#internal-events).
These are used to change the state of a permission request, by inserting new data into the event table and notifying any domain event handlers, but have to be ignored by integration event handlers.
Internal events should not change the `PermissionProcessStatus` of a permission request.
Since we are polling data, we can be sure that the permission request has the `ACCEPTED` status.

```java

@Entity(name = "LatestMeterReadingEvent")
public class LatestMeterReadingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
  private final ZonedDateTime latestReading;

  public CreatedEvent(String permissionId, ZonedDateTime latestReading) {
    super(permissionId, PermissionProcessStatus.ACCEPTED);
    this.latestReading = latestReading;
  }

  // No args constructor

  public ZonedDateTime latestReading() {
    return latestReading;
  }
}
```

Since the
`LatestMeterReadingEvent` introduces a new column to the permission event table, the table has to be updated too.
In order, for the migration to work, create a new SQL script in the migration directory of the region connector.
Add the newly added latest reading as a column.
Recreate the permission request view and the permission request class to include the `latest_reading` too.
This is not necessary here, since we are only going to use it in the event later on.

```sql
ALTER TABLE foo_bar.permission_event
  ADD latest_reading TIMESTAMP(6) WITH TIME ZONE;


CREATE OR REPLACE VIEW foo_bar.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   foo_bar.firstval_agg(connection_id) OVER w  AS connection_id,
                                   foo_bar.firstval_agg(data_need_id) OVER w   AS data_need_id,
                                   foo_bar.firstval_agg(status) OVER w         AS status,
                                   foo_bar.firstval_agg(data_start) OVER w     AS data_start,
                                   foo_bar.firstval_agg(data_end) OVER w       AS data_end,
                                   foo_bar.firstval_agg(granularity) OVER w    AS granularity,
                                   MIN(event_created) OVER w                   AS created,
                                   foo_bar.firstval_agg(latest_reading) OVER w AS latest_reading
FROM foo_bar.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
```

```java

@Entity
@Table(name = "permission_request", schema = "foo_bar")
public class FooBarPermissionRequest implements MeterReadingPermissionRequest {
  @Id
  @Column(name = "permission_id")
  private final String permissionId;
  @Column(name = "connection_id")
  private final String connectionId;
  @Column(name = "data_need_id")
  private final String dataNeedId;
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private final PermissionProcessStatus status;
  @Column(name = "granularity")
  @Enumerated(EnumType.STRING)
  private final Granularity granularity;
  @Column(name = "data_start")
  private final LocalDate start;
  @Column(name = "data_end")
  private final LocalDate end;
  @Column(name = "created")
  private final ZonedDateTime created;
  @Column(name = "latest_reading")
  @Nullable
  private ZonedDateTime latestReading;
  // Additional attributes here

  // no args constructor for hibernate

  @Override
  public Optional<LocalDate> latestMeterReadingEndDate() {
    return latestReading().map(ZonedDateTime::toLocalDate);
  }

  public Optional<ZonedDateTime> latestReading() {
    return Optional.ofNullable(latestReading);
  }

  // Overrides here
}
```

With the `LatestMeterReadingEvent` is rather simple to check if a permission request is fulfilled.
This event handler simply checks for fulfillment by comparing the latest reading DateTime to the end date of the permission request.

```java

@Component
public class LatestMeterReadingEventHandler implements EventHandler<LatestMeterReadingEvent> {
  private final Outbox outbox;
  private final FooBarPermissionRequestRepository repo;

  public LatestMeterReadingEventHandler(EventBus eventBus, Outbox outbox, FooBarPermissionRequestRepository repo) {
    this.outbox = outbox;
    this.repo = repo;
    eventBus.filteredFlux(LatestMeterReadingEvent.class)
            .subscribe(this::accept);
  }

  public void accept(LatestMeterReadingEvent event) {
    var pr = repo.getbyPermissionId(event.permissionId());
    if (DateTimeUtils.endOfDay(pr.end(), ZoneOffset.UTC).isAfter(event.latestReading())) {
      return;
    }
    outbox.commit(new FulfilledEvent(pr.permissionId()));
  }
}
```

Once the data is emitted to the `ValidatedHistoricalDataStream` it can be emitted to the outbound connectors.
To that an implementation of the [`RawDataProvider`](./api.md#rawdataprovider) is required.
The implementation subscribes to the `ValidatedHistoricalDataStream` and converts the
`IdentifiableValidatedHistoricalData` to a [`RawDataMessage`](../../2-integrating/messages/agnostic.md#raw-data-messages).
If the API responses are in JSON the default implementation [`JsonRawDataProvider`](./shared-functionality.md#jsonrawdataprovider) for JSON values can be used instead.

```java

@Component
public class FooBarRawDataProvider implements RawDataProvider {
  private final Flux<RawDataMessage> messages;

  public FooBarRawDataProvider(ValidatedHistoricalDataStream stream) {
    messages = stream.validatedHistoricalData()
                     .map(this::toRawDataMessage);
  }

  @Override
  public Flux<RawDataMessage> getRawDataStream() {
    return messages;
  }

  private RawDataMessage toRawDataMessage(IdentifiableValidatedHistoricalData identifiableData) {
    String rawString = // TODO: serialize identifiableData.payload() to a String
    return new RawDataMessage(identifiableData.permissionRequest(), rawString);
  }
}
```

## Map validated historical data to CIM document

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :arrow_right: Map validated historical data to validated historical data market documents
- Allow data needs for future data, and request the data once available
- Request accounting point data and emit it to raw data stream
- Map accounting point data to accounting point market document
- Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

The region connector can request validated historical data and emit it as raw data messages.
It can react to revocation of permissions by the final customer, by checking the error messages when requesting data from the MDA's API.
The next step is to map the validated historical data to the [validated historical data market document](../../2-integrating/messages/cim/validated-historical-data-market-documents.md).
Similar to the `RawDataProvider`, we implement an [`ValidatedHistoricalDataEnvelopeProvider`](./api.md#validatedhistoricaldataenvelopeprovider).
Since the mapping of the data to a CIM document depends on the data given, that part is left out as TODO.
There are some [helpers](./shared-functionality.md#cim-utilities-and-helper-classes) for the mapping available.

```java

@Component
public class FooBarValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {
  private final Flux<ValidatedHistoricalDataEnvelope> data;

  public FooBarValidatedHistoricalDataEnvelopeProvider(ValidatedHistoricalDataStream stream) {
    data = stream.validatedHistoricalData()
                 .map(this::toValidatedHistoricalDataMarketDocument);
  }

  public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
    return data;
  }

  private ValidatedHistoricalDataEnvelope toValidatedHistoricalDataMarketDocument(IdentifiableValidatedHistoricalData message) {
    ValidatedHistoricalDataMarketDocumentComplexType vhdDocument = // TODO: Convert message to validated historical data market document
    return new VhdEnvelope(vhdDocument, message.permissionRequest()).wrap();
  }
}
```

That's everything needed to create a validated historical data market document.

## Implement Future Data

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :arrow_right: Allow data needs for future data, and request the data once available
- Request accounting point data and emit it to raw data stream
- Map accounting point data to accounting point market document
- Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

Implementing future data is rather easy once requesting validated historical data is implemented.
This implementation periodically checks for new data for active permission requests.
It uses a cron expression that should be configured for the region connector.
To avoid redundant implementations for each region connector, a CommonFutureDataService was implemented. 
To use it, add a Bean to the region connectors spring configuration.

```java
@Bean
public CommonFutureDataService<FooPermissionRequest> commonFutureDataService(
        PollingService pollingService,
        BarPermissionRequestRepository repository,
        RegionConnectorMetadata metadata,
        TaskSchedular taskSchedular,
        DataNeedCalculationService<DataNeed> calculationService
){
  return new CommonFutureDataService<>(
          pollingService,
          repository,
          "0 0 17 * * *",
          metadata,
          taskSchedular,
          calculationService
  );
}
```

The `StartPollingEvent` is a simple internal event to trigger the `AcceptedHandler`.

```java

@Entity(name = "StartPollingEvent")
public class StartPollingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {

  public CreatedEvent(String permissionId) {
    super(permissionId, PermissionProcessStatus.ACCEPTED);
  }

  // No args constructor
}
```

Last update the `AcceptedHandler` to subscribe to the new type of event.

```java

@Component
public class AcceptedHandler implements EventHandler<PermissionEvent> {
  // members omitted

  public AcceptedHandler(
          EventBus eventBus,
          FooBarPermissionRequestRepository repo,
          ApiClient api,
          ValidatedHistoricalDataStream stream,
          Outbox outbox
  ) {
    this.repo = repo;
    this.api = api;
    this.stream = stream;
    this.outbox = outbox;
    eventBus.filteredFlux(AcceptedEvent.class)
            .subscribe(this::accept);
    // New code
    eventBus.filteredFlux(StartPollingEvent.class)
            .subscribe(this::accept);
  }
  // Methods omitted...
}
```

The region connector is now ready to poll data from the MDA for permission requests that specify data in the future.

## Accounting Point Data

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :arrow_right: Request accounting point data and emit it to raw data stream
- :arrow_right: Map accounting point data to accounting point market document
- Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

Requesting accounting point data is almost the same as [requesting validated historical data](#request-validated-historical-data).
Therefore, this section does not contain any code examples.
To request accounting point data include the
`AcountingPointDataNeed` in the list of supported data needs in your metadata implementation.
When you get a permission request for accounting point data, don't request validated historical data from the MDA, but accounting point data.
Emit the data via the `RawDataProvider` and the [`AccountingPointEnvelopeProvider`](./api.md#accountingpointenvelopeprovider).

## Ensure Data Needs are Enforced when Request Data

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :arrow_right: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

Ensuring that only the data specified by the data need is requested should not be forgotten.
This implementation already checks if permission requests are active and if it is a request for validated historical data or accounting point data.
The validated historical data need also specifies an energy type and granularities.
If the MDA cannot serve the data in the required granularities or the data for the wrong energy type, for example, gas readings instead of electricity, it should be discarded.
If none of the data fits the data need, the permission request has to be marked as `UNFULFILLABLE`.
Since there are so many places where this could be checked, there is no example code for this here.
Nevertheless, this should not be forgotten.

## Termination of Permission Requests

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :white_check_mark: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- :arrow_right: Allow termination of permission requests
  - Remove credentials if possible
  - Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

Once a permission request is accepted, it can be terminated by the eligible party for no reason.
We already implemented the interface for terminations, but a TODO comment was left instead of an actual implementation.

> [!Warning]
> Right now there is no dead letter queue for invalid permission requests.
> This is subject to change.

```java

@Component
public class FooBarRegionConnector implements RegionConnector {
  private final Outbox outbox;
  private final FooBarPermissionRequestRepository repo;

  public FooBarRegionConnector(Outbox outbox, FooBarPermissionRequestRepository repo) {
    this.outbox = outbox;
    this.repo = repo;
  }

  @Override
  public RegionConnectorMetadata getMetadata() {
    return FooBarRegionConnectorMetadata.getInstance();
  }

  @Override
  public void terminatePermission(String permissionId) {
    var pr = repo.findByPermissionId(permissionId);
    if (pr.isPresent() && pr.get().status() == PermissionProcessStatus.ACCEPTED) {
      outbox.commit(new TerminatedEvent(permissionId));
    }
  }
}
```

Now the permission request is terminated on EDDIE's side if the PA allows external termination, go to the next section.
If not, go to [remove credentials](#remove-credentials).

## Externally Terminate Permission Requests

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :white_check_mark: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- :white_check_mark: Allow termination of permission requests
  - Remove credentials if possible
  - :arrow_right: Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

If the PA allows external termination of permission requests, the region connector should implement that functionality.

```java

@Component
public class TerminationHandler implements EventHandler<TerminatedEvent> {
  private final Outbox outbox;

  public TerminationHandler(Outbox outbox, EventBus eventBus) {
    this.outbox = outbox;
    eventBus.filteredFlux(TerminatedEvent.class)
            .subscribe(this::accept);
  }

  public void accept(TerminatedEvent event) {
    // TODO: check if permission request needs to be externally terminated
    outbox.commit(new RequiresExternalTermination(event.permissionId()));
  }
}
```

If external termination is required, send the termination to the PA.

```java

@Component
public class RequiresExternalTerminationHandler implements EventHandler<RequiresExternalTerminatedEvent> {
  private final Outbox outbox;
  private final ApiClient api;
  private final FooBarPermissionRequestRepository repo;

  public RequiresExternalTerminationHandler(
          Outbox outbox,
          ApiClient api,
          FooBarPermissionRequestRepository repo,
          EventBus eventBus
  ) {
    this.outbox = outbox;
    this.api = api;
    this.foo = foo;
    eventBus.filteredFlux(TerminatedEvent.class)
            .subscribe(this::accept);
  }

  public void accept(RequiresExternalTerminatedEvent event) {
    var pr = repo.getByPermissionId(event.permissionId());
    api.terminate(pr)
       .subscribe(success -> outbox.commit(new ExternallyTerminatedEvent(event.permissionId())),
                  error -> outbox.commit(new FailedToTerminatedEvent(event.permissionId())));
  }
}
```

The logic to retry termination when it fails is exactly the same as for the failed to send event, this part is, therefore, omitted.

## Remove Credentials

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :white_check_mark: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- :white_check_mark: Allow termination of permission requests
  - :arrow_right: Remove credentials if possible
  - :white_check_mark: Terminate on the permission administrators side if possible
- Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

When a permission request was terminated, externally terminated, fulfilled, or is unfulfillable, any credentials saved in the database should be removed.
Therefore, it is better to save any credentials in its own database table that allows delete operations and not in the permission event table.

> [!WARNING]
> Don't store credentials in the [permission event table](./internal-architecture.md#event-store).

```java

@Component
public class PermissionRequestFinishedHandler implements EventHandler<PermissionEvent> {

  public RequiresExternalTerminationHandler(
          Outbox outbox,
          ApiClient api,
          FooBarPermissionRequestRepository repo,
          EventBus eventBus
  ) {
    // TODO: subscribe to any events that require removal of credentials
    eventBus.filteredFlux(PermissionProcessStatus.EXTERNALLY_TERMINATED)
            .subscribe(this::accept);
  }

  public void accept(PermissionEvent event) {
    // TODO: delete any credentials
  }
}
```

## Allow retransmission of validated historical data for a specific permission request

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :white_check_mark: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- :white_check_mark: Allow termination of permission requests
  - :white_check_mark: Remove credentials if possible
  - :white_check_mark: Terminate on the permission administrators side if possible
- :right_arrow: Allow retransmission of validated historical data for a specific permission request
- Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

To implement the retransmission requests for validated historical data for a region connector, the [RegionConnectorRetransmissionService](./api.md#regionconnectorretransmissionservice) must be implemented.
It allows the eligible party to request validated historical data again, after it was already requested by the region connector.
The reasons for this can vary, for example, the eligible party is missing a specific timeframe of the data.
There is already a shared implementation, that validates the retransmission request and requests the data from the polling service of the region connector.

```java
@Configuration
public class FooSpringConfig{
  @Bean
  public RetransmissionValidation retransmissionValidation(RegionConnectorMetadata metadata, DataNeedsService dataNeedsService) {
    return new RetransmissionValidation( metadata, dataNeedsService );
  }
  @Bean
  public CommonRetransmissionService<FooPermissionRequest> retransmissionService(
          BarPermissionRequestRepository repository,
          PollingService pollingService,
          RetransmissionValidation validation
  ){
    return new CommonFutureDataService<>( repository, pollingService, validation);
  }
}
```

To request the data from the API, the polling service needs to implement the `PollingFunction`, which can be implemented in any service that polls the data.
Important is that the polling function emits the data to the outbound connectors using whatever mechanisms are present in the region connector.

```java
import java.time.ZonedDateTime;

@Service
public class PollingService implements PollingFunction<FooPermissionRequest> {
  private final ApiClient api;
  private final ValidatedHistoricalDataStream stream;

  public PollingService(ApiClient api, ValidatedHistoricalDataStream stream) {
    this.api = api;
    this.stream = stream;
  }

  @Override
  public Mono<RetransmissionResult> poll(
          FooPermissionRequest permissionRequest,
          RetransmissionRequest retransmissionRequest
  ) {
    return api.meteredData(pr, retransmissionRequest.from(), retransmissionRequest.to())
              .doOnSuccess(result -> stream.publish(pr, result))
              .onErrorResume(error -> new Failure(permissionRequest.permissionId, ZonedDateTime.now(), error.getMessage()))
              .map(ignored -> new Success(permissionRequest.permissionId(), ZonedDateTime.now()));
  }
}
```


## Timing Out Permission Requests

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :white_check_mark: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- :white_check_mark: Allow termination of permission requests
  - :white_check_mark: Remove credentials if possible
  - :white_check_mark: Terminate on the permission administrators side if possible
- :white_check_mark: Allow retransmission of validated historical data for a specific permission request
- :arrow_right: Time out stale permission requests
- Implement health indicators for external APIs and services
  :::

Sometimes final customers ignore permission requests; since those can't be used to retrieve any data, they should be time outed.
Luckily, there is a [shared implementation](./shared-functionality.md#commontimeoutservice) for that.
It can be quickly spun up, and handles all the time-outs, but it requires the [`StalePermissionRequestRepository`](./api.md#database-access-to-permission-requests).

```java

@EnableScheduling
public class FooBarSpringConfig {
  @Bean
  public CommonTimeoutService(
          FooPermissionRequestRepository repo,
          Outbox outbox,
          TimeoutConfiguration config // injected from the core
  ) {
    return new CommonTimeoutService(
            repo,
            SimpleEvent::new,
            // Factory to create permission events based on permission ID and a permission process status
            outbox,
            config
    );
  }
}
```

```java

@Entity(name = "SimpleEvent")
public class SimpleEvent extends PersistablePermissionEvent {

  public CreatedEvent(String permissionId, PermissionProcessStatus status) {
    super(permissionId, status);
  }

  // No args constructor
}
```

The `FooBarPermissionRequestRepository` has to be extended for the timeout service to work.
Since it needs to implement all repository interfaces provided by the API, we can implement the
`FullPermissionRequestRepository`.

```java

@org.springframework.stereotype.Repository
public interface FooBarPermissionRequestRepository
        extends FullPermissionRequestRepository<FooBarPermissionRequest>,
        Repository<FooBarPermissionRequest, String> {

  @Query(
          value = "SELECT * FROM foo_bar.permission_request WHERE status = 'SENT_TO_PERMISSION_ADMINISTRATOR' AND created <= NOW() - :hours * INTERVAL '1 hour'",
          nativeQuery = true
  )
  @Override
  List<FooBarPermissionRequest> findStalePermissionRequests(@Param("hours") int duration);
}
```

## Health Indicators

::: details Checklist Status

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :white_check_mark: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- :white_check_mark: Allow termination of permission requests
  - :white_check_mark: Remove credentials if possible
  - :white_check_mark: Terminate on the permission administrators side if possible
- :white_check_mark: Allow retransmission of validated historical data for a specific permission request
- :white_check_mark: Time out stale permission requests
- :arrow_right: Implement health indicators for external APIs and services
  :::

Last, the region connector should provide at least one health indicator, that indicates the status of the PA and/or MDA.
Here is a simple example that assumes that the API provides a health endpoint.

```java

@Component
public class FooBarHealthIndicator implements HealthIndicator {
  private final ApiClient api;

  public GreenButtonApiHealthIndicator(
          ApiClient api
  ) {
    this.api = api;
  }

  @Override
  public Health health() {
    return api.isAlive()
              .map(isAlive -> Boolean.TRUE.equals(isAlive) ? Health.up() : Health.down())
              .onErrorResume(Exception.class, e -> Mono.just(Health.down(e)))
              .onErrorResume(e -> Mono.just(Health.down()))
              .map(Health.Builder::build)
              .block();
  }
}
```

## Finished

Everything from the checklist is implemented now.
Of course, region connectors can be extended to include more functionality from the MDA and PA.
If any errors in this document are found, please let us know or [edit the document yourself](../documentation.md).

- :white_check_mark: Create a permission request at the permission administrators side
- :white_check_mark: Implement custom element for region connector
- :white_check_mark: Implement permission market documents
- :white_check_mark: Request validated historical data and emit it to raw data stream
- :white_check_mark: Map validated historical data to validated historical data market documents
- :white_check_mark: Allow data needs for future data, and request the data once available
- :white_check_mark: Request accounting point data and emit it to raw data stream
- :white_check_mark: Map accounting point data to accounting point market document
- :white_check_mark: Ensure that data needs are enforced by region connector, such as only requesting the correct data.
  For example, not requesting gas metered data for data need that specifies electricity.
- :white_check_mark: React to revocation of permission request
- :white_check_mark: Allow termination of permission requests
  - :white_check_mark: Remove credentials if possible
  - :white_check_mark: Terminate on the permission administrators side if possible
- :white_check_mark: Allow retransmission of validated historical dat for a specific permission request
- :white_check_mark: Time out stale permission requests
- :white_check_mark: Implement health indicators for external APIs and services
