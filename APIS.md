# EDDIE Framework external API manual

The APIs described here are accessible through the management interface. These URLs are accessible through the dedicated
management port (`management.server.port` property, default 9090) and the `/management` URL prefix, e.g.:
<http://localhost:9090/management>

## Data need API

When configured, the EDDIE framework reads the used data needs from its own database. These can be provided by
this REST-ful API. Data needs are described in the
[logical data model](https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/).

### URLs and methods

| URL                        | method   | description                                                                |
|----------------------------|----------|----------------------------------------------------------------------------|
| /management/data-need      | GET      | returns a list of all data needs                                           |
| /management/data-need      | POST/PUT | creates new data need from payload                                         |
| /management/data-need/{id} | GET      | returns a single data need                                                 |
| /management/data-need/{id} | POST     | updates the data need, the URL id has to match the id property in the body |
| /management/data-need/{id} | DELETE   | deletes the data need if present, always returns 200 (ok)                  |

### Payload structure

**A single data need is structured like this:**

A request like `GET http://localhost:9090/management/data-need/4711` can deliver a `Content-Type: application/json` with
this payload:

```
{
    "id": "4711",
    "description": "Some arbitrary data need",
    "type": "HISTORICAL_VALIDATED_CONSUMPTION_DATA",
    "granularity": "PT1H",
    "durationStart": -33,
    "durationOpenEnd": false,
    "durationEnd": 0
}
```

**When querying all data needs**
A JSON array is returned containing the individual data needs. E.g. `GET http://localhost:9090/management/data-need`
could result in:

```
[
    {
        "id": "4711", "description": "Some arbitrary data need",
        "type": "HISTORICAL_VALIDATED_CONSUMPTION_DATA", "granularity": "PT1H",
        "durationStart": -33, "durationOpenEnd": false, "durationEnd": 0
    },
    {
        "id": "XYZ", "description": "Realtime data",
        "type": "SMART_METER_P1_DATA", "granularity": "PT5M",
        "durationStart": 0, "durationOpenEnd": true
    },
]
```
