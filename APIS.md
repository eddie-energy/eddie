# EDDIE Framework external API manual

## Data need API

When configured, the EDDIE framework reads the used data needs from its own database. These can be provided by
this REST-ful API. Data needs are described in the
[logical data model](https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/).

### URLs and methods

| URL                        | method   | description                        |
|----------------------------|----------|------------------------------------|
| /management/data-need      | GET      | returns a list of all data needs   |
| /management/data-need      | POST/PUT | creates new data need from payload |
| /management/data-need/{id} | GET      | returns a single data need         |
| /management/data-need/{id} | POST     | updates the data need              |

### Payload structure

**A single data need is structured like this:**
Content-Type: application/json

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
A JSON array is returned containing the individual data needs.
