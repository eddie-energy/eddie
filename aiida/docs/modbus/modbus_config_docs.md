
# Modbus Configuration Documentation

## Overview

This document describes the structure and components of a Modbus device configuration YAML used in AIIDA. It supports features like:
- Multi-category sources
- Endian control
- Virtual datapoints
- Transformations and translations
- Read/write access specification

---

## Top-Level Structure

```yaml
modbus:
  devices:
    - id: "..."
      name: "..."
      port: ...
      unitId: ...
      intervals:
        read:
          default: ...
          min: ...
      sources: [...]
```

- **id**: Unique identifier for the device.
- **name**: Human-readable device name.
- **port**: TCP port of the Modbus device.
- **unitId**: Modbus unit identifier.
- **intervals.read.default/min**: Default and minimum polling intervals in milliseconds.
- **sources**: List of logical data sources grouped by category.

---

## Sources

```yaml
sources:
  - category: "inverter"
    id: "inverter-1"
    datapoints: [...]
```

- **category**: One of: `INVERTER`, `BATTERY`, `ELECTRICITY_METER_AC`, `ELECTRICITY_METER_DC`, `PV`, `CHARGING_STATION_AC`, `CHARGING_STATION_DC`, `UNKNOWN`.
- **id**: Unique identifier for the logical source.
- **datapoints**: List of data entries for this source.

---

## Datapoint Structure

```yaml
- id: "status"
  register: 10
  registerType: "holding"
  valueType: "uint16"
  length: 1
  endian: "big"
  access: "read"
  translations: {...}
```

- **id**: Identifier for the datapoint.
- **register**: Modbus register address.
- **registerType**: One of `holding`, `input`, `coil`, or `discrete`.
- **valueType**: One of `uint16`, `int16`, `float32`, `int32`, `string`, `boolean`.
- **length**: Number of Modbus registers used (1 register = 2 bytes).
- **endian**: Either `big` or `little`.
- **access**: One of `read`, `write`, or `readwrite`.
- **translations** *(optional)*: Maps raw register values to human-readable labels.
- **transform** *(optional)*: Mathematical expression to transform the raw value (see below).

---

## Virtual Datapoints

```yaml
- id: "power_total"
  virtual: true
  source: [ "voltage_l1", "current_l1", ... ]
  transform: "(@voltage_l1 * @current_l1) + ..."
  access: "read"
```

- **virtual**: Must be `true`.
- **source**: List of other datapoint IDs used in the transformation.
- **transform**: MVEL expression using `@` notation to refer to source values.

Example with external references:
```yaml
transform: "@battery-1::state_of_charge_lit"
```

---

## Transform Expression Language

- Standard arithmetic: `+`, `-`, `*`, `/`
- Ternary logic: `condition ? trueVal : falseVal`
- Boolean comparisons: `==`, `!=`, `>`, `<`, `>=`, `<=`

Example:
```yaml
transform: "(@power_total + @battery-1::state_of_charge_lit) < 8000 ? 'low' : 'high'"
```

---

## Translations

```yaml
translations:
  "1": "ON"
  "2": "STANDBY"
  "3": "FAULT"
  "default": "UNKNOWN"
```

Used for converting raw numeric values into user-friendly labels.

---

## Access Modes

- **read**: Value can be read from the device.
- **write**: Value can be written to the device.
- **readwrite**: Bi-directional.

---

## Notes

- Use `::` to reference datapoints across sources, e.g., `battery-1::state_of_charge_lit`.
- Virtual datapoints support chained expressions combining multiple data sources.
- Comments can be used to disable transformations for testing or documentation.

---
