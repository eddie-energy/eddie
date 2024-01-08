# EDDIE Framework external API manual

The APIs described here are accessible through the management interface. These URLs are accessible through the dedicated
management port (`management.server.port` property, default 9090) and the `/management` URL prefix, e.g.:
<http://localhost:9090/management>

## Data need API

When configured, the EDDIE framework reads the used data needs from its own database. These can be provided by
this REST-ful API. Data needs are described in the
[logical data model](https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/).

### URLs and methods

| URL                         | method | description                                                                                              |
|-----------------------------|--------|----------------------------------------------------------------------------------------------------------|
| /management/data-needs      | GET    | returns a list of all data needs                                                                         |
| /management/data-needs      | POST   | creates new data need from payload                                                                       |
| /management/data-needs/{id} | GET    | returns a single data need                                                                               |
| /management/data-needs/{id} | PUT    | updates the data need, the URL id has to match the id property in the body                               |
| /management/data-needs/{id} | DELETE | deletes the data need if present, returns 204 if data need got deleted and 404 if no data need was found |

### Payload structure

**A single data need is structured like this:**

A request like `GET http://localhost:9090/management/data-needs/4711` can deliver a `Content-Type: application/json`
with
this payload:

```
{
    "id": "4711",
    "description": "Some arbitrary data need",
    "type": "HISTORICAL_VALIDATED_CONSUMPTION_DATA",
    "granularity": "PT1H",
    "durationStart": -33,
    "durationOpenEnd": false,
    "durationEnd": 0,
    "transmissionInterval": null,
    "sharedDataIds": [],
    "serviceName": null
}
```

**When querying all data needs**
A JSON array is returned containing the individual data needs. E.g. `GET http://localhost:9090/management/data-needs`
could result in:

```
[
    {
        "id": "4711", "description": "Some arbitrary data need",
        "type": "HISTORICAL_VALIDATED_CONSUMPTION_DATA", "granularity": "PT1H",
        "durationStart": -33, "durationOpenEnd": false, "durationEnd": 0,
        "transmissionInterval": null, "sharedDataIds": [], "serviceName": null
    },
    {
        "id": "XYZ", "description": "Realtime data",
        "type": "AIIDA_NEAR_REALTIME_DATA", "granularity": "PT5M",
        "durationStart": 0, "durationOpenEnd": true,
        "transmissionInterval": null, "sharedDataIds": [], "serviceName": null
    }
]
```

## OpenAPI specification

The API documentation is also available as an [OpenAPI](https://swagger.io/specification/) specification. It can be
found under [openapi/management-data-needs.yaml](openapi/management-data-needs.yaml)

View it in an IDE or copy the contents of the file and paste it into the [Swagger Editor](https://editor.swagger.io/) to
view the API specification.