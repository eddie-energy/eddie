# Changelog for the Common Information Model

This changelog describes the changes made to the CIM maven artifact.
Inspired by [common-changelog](https://github.com/vweevers/common-changelog).

## 0.0.0 - 2025-05-07

Introduction of the changelog for the CIM.
The CIM model now contains classes for CIM v0.82 and v0.91.08.
For more information, see [Common Information Model Client Libraries](https://architecture.eddie.energy/framework/2-integrating/messages/cim/client-libraries.html).

## 1.0.0 - 2025-05-15

- Add validated historical data for CIM version `0.91.08`.
- Flatten the directory structure for version `0.91.08` to not duplicate generated classes.
- This moves the redistribution transaction requests from the package `energy.eddie.cim.v0_91_08.retransmission` to the package `energy.eddie.cim.v0_91_08`.

## 2.0.0 - 2025-05-22

- Change enumeration name for generated CIM classes
- Change serialization for xs:datetime

## 2.0.1 - 2025-05-27

- Change artifact ID to `cim` from `cim-test`
- Set the correct version for the artifact

## 2.1.0 - 2025-06-30

- Add (near) real-time data for CIM version `1.04`.
- AIIDA now depends on this version of the CIM.

## 3.0.0 - 2025-07-14

- Replace validated historical data market document version `0.91.08` with version `1.04`
- Move realtime data schemas to its own package and folder

## 3.0.1 - 2025-07-15

- Truncate datetime to seconds as required by CIM's ESMP datetime

## 3.1.0 - 2025-07-22

- Add Accounting Point Data Market Document Schema v1.04
- Add Permission Market Document Schema v1.04

## 3.2.0 - 2025-11-07

- Add serialization and deserialization capabilities for CIM messages for JSON and XML format

## 3.2.1 - 2026-01-15

- Update Jackson dependencies to Jackson 3

## 3.2.2 - 2026-01-26

- Add copyright notices to source files

## 3.3.0 - 2025-xx-xx
- Add (near) real-time data for CIM version `1.12`
- Fixes wrong wording of `QuantityTypeKind` values
- Adds additional `QuantityTypeKind` values
- AIIDA now depends on this version of the CIM

[//]: # (TODO: Adapt the changelog before pushing to main)
