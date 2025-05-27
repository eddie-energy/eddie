# Changelog for the Common Information Model

This changelog describes the changes made to the CIM maven artifact.
Inspired by [common-changelog](https://github.com/vweevers/common-changelog).

## 0.0.0 - 2025-05-07

Introduction of the changelog for the CIM.
The CIM model now contains classes for CIM v0.82 and v0.91.08.
For more information, see [Common Information Model Client Libraries](https://eddie-web.projekte.fh-hagenberg.at/framework/2-integrating/messages/cim-client-libraries.html#common-information-model-client-libraries).

## 1.0.0 - 2025-05-15

- Add validated historical data for CIM version `0.91.08`.
- Flatten the directory structure for version `0.91.08` to not duplicate generated classes.
- This moves the redistribution transaction requests from the package `energy.eddie.cim.v0_91_08.retransmission` to the package `energy.eddie.cim.v0_91_08`.

## 2.0.0 - 2025-05-22

- Change enumeration name for generated CIM classes
- Change serialization for xs:datetime

## 2.0.1 - 2025-05-27

- Change artifact ID to `cim` from `cim-test`
