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

## 3.3.0 - 2026-02-13
- Add (near) real-time data for CIM version `1.12`
- Fixes wrong wording of `QuantityTypeKind` values
- Adds additional `QuantityTypeKind` values
- Separates `MetaInformation` from `MessageDocumentHeader` into own class
- Adds `Asset` to `MetaInformation` in `MessageDocumentHeader`
- Adds `Accounting Point` to `TimeSeries`
- AIIDA now depends on this version of the CIM

## 3.4.0 - 2026-02-16

- Add Reference Energy Curve Min Max Operating Envelope Market Document Schema v1.12

## 3.5.0 - 2026-02-20

- Add Acknowledgement Document Schema v1.12

## 3.5.1 - 2026-02-26

- Improve Javadoc in the `CommonInformationModelVersions` class.

## 3.6.0 - 2026-03-10

- Add schema definitions for agnostic opaque envelopes

## 3.7.0 - 2026-03-24

- Add agnostic message classes

## 3.8.0 - 2026-04-07

- Add EnergySharingReferenceDataDocument.
  This document can be used to announce the current participation factor of an accounting point for a specific collective energy sharing unit (CESU).
- Fix missing (de)serialization for the following CIM documents:
  - Real Time Data Market Document v1.12
  - Acknowledgement Document v1.12
  - Reference Energy Curve Min-Max Operation Document v1.12
- Fix typo in RealTimeData Document namespace, where the namespace is missing a colon (:) in the namespace declaration

## 3.9.0 - 2026-06-02

- Add agnostic `PermissionCommand` for transmission control sent by the eligible party.
  Modeled as a sealed interface discriminated on `action`, with the subtypes `UpdateSchedule`,  `SetTransmissionEnabled`, and `Terminate`.
- Add JSON and XSD schema definitions for the permission command.

## 3.9.1 - 2026-06-08

- Rename `UpdateSchedule` to `UpdateTransmissionSchedule` for clarity.
- Add `PermissionCommand.Action` enum (`UPDATE_TRANSMISSION_SCHEDULE`, `SET_TRANSMISSION_ENABLED`, `TERMINATE`).
  `action()` now returns `Action` instead of `String` (use `action().name()` for the wire value).
- Add `Action.requiresExplicitGrant()`: `true` for commands a data need must opt into, `false` for `TERMINATE`.