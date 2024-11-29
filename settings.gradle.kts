rootProject.name = "eddie"

plugins {
    id("com.autonomousapps.build-health") version ("2.5.0")
}

include("region-connectors")

include("api")
findProject("api")?.name = "api"

include("core")
findProject("core")?.name = "core"

include("region-connectors:region-connector-aiida")
findProject(":region-connectors:region-connector-aiida")?.name = "region-connector-aiida"

include("region-connectors:region-connector-at-eda")
findProject(":region-connectors:region-connector-at-eda")?.name = "region-connector-at-eda"

include("region-connectors:region-connector-be-fluvius")
findProject(":region-connectors:region-connector-be-fluvius")?.name = "region-connector-be-fluvius"

include("region-connectors:region-connector-dk-energinet")
findProject(":region-connectors:region-connector-dk-energinet")?.name = "region-connector-dk-energinet"

include("region-connectors:region-connector-fr-enedis")
findProject(":region-connectors:region-connector-fr-enedis")?.name = "region-connector-fr-enedis"

include("region-connectors:region-connector-es-datadis")
findProject(":region-connectors:region-connector-es-datadis")?.name = "region-connector-es-datadis"

include("region-connectors:region-connector-nl-mijn-aansluiting")
findProject(":region-connectors:region-connector-nl-mijn-aansluiting")?.name = "region-connector-nl-mijn-aansluiting"

include("region-connectors:region-connector-fi-fingrid")
findProject(":region-connectors:region-connector-fi-fingrid")?.name = "region-connector-fi-fingrid"

include("region-connectors:region-connector-simulation")
findProject(":region-connectors:region-connector-simulation")?.name = "region-connector-simulation"

include("region-connectors:region-connector-us-green-button")
findProject(":region-connectors:region-connector-us-green-button")?.name = "region-connector-us-green-button"

include("region-connectors:shared")
findProject(":region-connectors:shared")?.name = "shared"

include("outbound-connectors:outbound-kafka")
findProject(":outbound-connectors:outbound-kafka")?.name = "outbound-kafka"

include("outbound-connectors:outbound-amqp")
findProject(":outbound-connectors:outbound-amqp")?.name = "outbound-amqp"

include("examples:example-app")
findProject(":examples:example-app")?.name = "example-app"

include("e2e-tests")
findProject("e2e-tests")?.name = "e2e-tests"

include("data-needs")

include("aiida")

include("european-masterdata")

include("admin-console")

include("examples:new-example-app")
include("outbound-connectors:outbound-shared")
findProject(":outbound-connectors:outbound-shared")?.name = "outbound-shared"
