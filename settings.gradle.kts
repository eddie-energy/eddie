rootProject.name = "eddie"

plugins {
    id("com.autonomousapps.build-health") version ("2.5.0")
}

include("region-connectors")

include("cim")

include("api")

include("core")

include("region-connectors:region-connector-aiida")
include("region-connectors:region-connector-at-eda")
include("region-connectors:region-connector-be-fluvius")
include("region-connectors:region-connector-cds")
include("region-connectors:region-connector-dk-energinet")
include("region-connectors:region-connector-es-datadis")
include("region-connectors:region-connector-fi-fingrid")
include("region-connectors:region-connector-fr-enedis")
include("region-connectors:region-connector-nl-mijn-aansluiting")
include("region-connectors:region-connector-si-moj-elektro")
include("region-connectors:region-connector-simulation")
include("region-connectors:region-connector-us-green-button")
include("region-connectors:shared")

include("region-connectors:region-connector-de-eta")

include("outbound-connectors:outbound-amqp")
include("outbound-connectors:outbound-kafka")
include("outbound-connectors:outbound-metric")
include("outbound-connectors:outbound-rest")
include("outbound-connectors:outbound-shared")

include("examples:example-app")
include("e2e-tests")
include("data-needs")

include("aiida")

include("european-masterdata")

include("admin-console")

include("examples:new-example-app")
