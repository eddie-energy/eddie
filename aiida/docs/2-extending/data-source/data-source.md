# Add a data source

> [!NOTE]
> This section explains how to create a new data source using the **Shelly data source** as an example.

In order to create a new data source in AIIDA, you typically need to implement the following components:

- [Data Source Type](#data-source-type)
- [DTO](#dto)
- [Model](#model)
- [Adapter](#adapter)
- [Testing](#testing)

## Data Source Type

Start by defining a new data source type in the `DataSourceType.Identifiers` class:

```java
package energy.eddie.aiida.models.datasource;

public enum DataSourceType {
    // ... other data source types
    SHELLY(Identifiers.SHELLY, "Shelly");
    // ... existing code

    public static class Identifiers {
        // ... other identifiers
        public static final String SHELLY = "SHELLY";
        // ... existing code
    }
}
```

## DTO

Create a DTO (in [`java/.../dtos/datasource`](https://github.com/eddie-energy/eddie/tree/main/aiida/src/main/java/energy/eddie/aiida/dtos/datasource)) to define the structure for creating or updating the data source.
All DTOs extend the abstract base class `dtos.datasource.DataSourceDto`.

If no additional fields are required, the DTO can be left empty:

```java
package energy.eddie.aiida.dtos.datasource.mqtt.shelly;

public class ShellyDataSourceDto extends DataSourceDto {}
```

Each DTO must be registered in the `DataSourceDto` base class so that it can be correctly deserialized:

```java
package energy.eddie.aiida.dtos.datasource;

@JsonSubTypes({
        // ... other data source types
        @JsonSubTypes.Type(value = ShellyDataSourceDto.class, name = DataSourceType.Identifiers.SHELLY),
})
// ... other annotations
public abstract class DataSourceDto {
    // ... existing code
}
```

If the data source requires custom properties, simply extend the DTO:

```java
@SuppressWarnings({"NullAway.Init"})
public class ShellyDataSourceDto extends DataSourceDto {
    @JsonProperty
    protected String myCustomProperty;

    public String myCustomProperty() {
        return myCustomProperty;
    }
}
```

## Model

Next, create the entity class (in [`java/.../models/datasource`](https://github.com/eddie-energy/eddie/tree/main/aiida/src/main/java/energy/eddie/aiida/models/datasource))) representing the data source in the database.
Extend the abstract base class `models.datasource.DataSourceModel` (or `MqttDataSource` for MQTT-based sources):

```java
package energy.eddie.aiida.models.datasource.mqtt.shelly;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SHELLY)
public class ShellyDataSource extends MqttDataSource {
    @SuppressWarnings("NullAway")
    protected ShellyDataSource() {}

    public ShellyDataSource(ShellyDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }
}
```

If the default MQTT topic structure is insufficient, override `updateMqttSubscribeTopic()`:

```java
@Override
private void updateMqttSubscribeTopic() {
    this.mqttSubscribeTopic = "PREFIX/" + id + "/SUFFIX";
}
```

If the password should not be generated automatically, override `setMqttPassword()`:

```java
@Override
public void setMqttPassword(String password) {
    // ignore, password is fixed
}
```

Register the model in the `createFromDto()` method:

```java
public static DataSource createFromDto(DataSourceDto dto, UUID userId) {
    return switch (dto) {
        // ... other data source DTOs
        case ShellyDataSourceDto parsedDto -> new ShellyDataSource(parsedDto, userId);
    };
}
```

Since the inheritance strategy is `SINGLE_TABLE`, all properties are stored in the same table.
Custom properties can be added as follows:

```java
public class ShellyDataSource extends MqttDataSource {
    @JsonProperty
    protected String myCustomProperty;
    
    public ShellyDataSource(ShellyDataSourceDto dto, UUID userId) {
        super(dto, userId);
        this.myCustomProperty = dto.myCustomProperty();
    }
    
    // ... other methods
}
```

Don’t forget to add the new column via a migration (in [`resources/db/aiida/migration`](https://github.com/eddie-energy/eddie/tree/main/aiida/src/main/resources/db/aiida/migration)):

```sql
ALTER TABLE data_source ADD COLUMN my_custom_property VARCHAR;
```

## Adapter

The adapter converts incoming data into AIIDA’s internal data format.
MQTT-based adapters extend `MqttDataSourceAdapter` which extends the `DataSourceAdapter`.

Create the adapter class (in [`java/.../adapters/datasource`](https://github.com/eddie-energy/eddie/tree/main/aiida/src/main/java/energy/eddie/aiida/adapters/datasource)):

```java
package energy.eddie.aiida.adapters.datasource.shelly;

public class ShellyAdapter extends MqttDataSourceAdapter<ShellyDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellyAdapter.class);
    private final ObjectMapper mapper;
    
    public ShellyAdapter(
            ShellyDataSource dataSource,
            ObjectMapper mapper,
            MqttConfiguration mqttConfiguration
    ) {
        super(dataSource, LOGGER, mqttConfiguration);
        this.mapper = mapper;
    }
    
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);

        var payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
        try {
            var json = mapper.readValue(payload, ShellyJson.class);
            
            // Convert to measurements

            emitAiidaRecord(dataSource.asset(), measurement.toAiidaRecordValue());
        } catch (IOException e) {
            LOGGER.error("Error while deserializing payload received from adapter. Payload was {}", payload, e);
        }
    }
}
```

Optionally, override the `health()` method to report adapter status.

Register the adapter in `DataSourceAdapter`:

```java
package energy.eddie.aiida.adapters.datasource;

public abstract class DataSourceAdapter<T extends DataSource> implements AutoCloseable, HealthIndicator {
    // ... existing code
    
    public static DataSourceAdapter<? extends DataSource> create(
            DataSource dataSource,
            ObjectMapper objectMapper,
            MqttConfiguration mqttConfiguration
    ) {
        return switch (dataSource) {
            // ... other data source adapters
            case ShellyDataSource ds -> new ShellyAdapter(ds, objectMapper, mqttConfiguration);
        };
    }
}
```

## Testing

Create unit and integration tests to verify that the new data source and adapter behave as expected.

## User Interface

If your new data source DTO uses fields not used by other data sources, like the `pollingInterval` for Modbus data sources, you will need to do the following to make it work properly with the user interface:

### Types

Update the AiidaDataSource type in [types.d.ts](https://github.com/eddie-energy/eddie/blob/main/aiida/ui/src/types.d.ts) to handle the optional props:

```js
export type AiidaDataSource = {
  id: string
  dataSourceType: string
  asset: string
  name: string
  // .... other props
  //DatasourceType = YOUR_NEW_DATASOURCE
  newField?: string 
}
```

### DataSourceModal

Update the form in the [DataSourceModal](https://github.com/eddie-energy/eddie/blob/main/aiida/ui/src/components/Modals/DataSourceModal.vue) component to include any new input fields for your new data source props. These extra fields should be put into the `extra-column` and only be displayed if the selected `dataSourceType` matches your type:

```html
<template>
  <ModalDialog>
    <form
    ...

    <Transition name="extra-column">
        <div
          class="column"
          v-if="dataSourceTypesWithExtraField.includes(dataSource.dataSourceType)"
        >
            <template v-if="dataSource.dataSourceType === 'MyNewType'">
                <div class="input-field extra-margin">
                    <label for="myNewField">
                        {{ t('datasources.modal.newField') }}
                    </label>
                    <!--- Make sure to choose an appropriate input tag -->
                    <input
                        id="myNewField"
                        v-model="dataSource.newField"
                        name="myNewField"
                        placeholder="add my new field"
                        required
                    />
                    <p v-if="errors['myNewField']" class="error-message">
                        {{ errors['myNewField'] }}
                    </p>
                </div>
            </template>
```

In addition validation for new fields should also be added in the `validateForm()` function:

```js
const validateForm = () => {
  ...
  if (dataSource.value?.dataSourceType === 'MyNewType') {
    handleRequired(
      dataSource.value?.newField,
      t('datasources.modal.newField'),
      'myNewField',
    )
  }
}
```

### DataSourceCard

As a last step you need to update the [DataSourceCard](https://github.com/eddie-energy/eddie/blob/main/aiida/ui/src/components/DataSourceCard.vue) component to include the new field(s).

```html
<script>
...

const {
  countryCode,
  asset,
  dataSourceType,
  enabled,
  id,
  externalHost,
  topic,
  username,
  name,
  pollingInterval,
  icon,
} = dataSource

...
</script>

<template>
  <article class="card" :class="{ 'is-open': isOpen }">
    ...

    <dl class="fields" :class="{ 'with-image': image }">
      ...
        <template v-if="newField">
            <div>
                <dt>{{ t('datasources.card.newField') }}</dt>
                <dd>{{ newField }}</dd>
            </div>
        </template>
```

## Documentation

Finally, document the new data source in the AIIDA documentation (see [extend data source documentation](documentation.md)).
