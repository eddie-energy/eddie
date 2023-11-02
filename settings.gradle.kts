rootProject.name = "eddie"
include("region-connectors")

include("api")
findProject("api")?.name = "api"

include("region-connectors:region-connector-at-eda")
findProject(":region-connectors:region-connector-at-eda")?.name = "region-connector-at-eda"

include("region-connectors:region-connector-dk-energinet")
findProject(":region-connectors:region-connector-dk-energinet")?.name = "region-connector-dk-energinet"

include("region-connectors:region-connector-fr-enedis")
findProject(":region-connectors:region-connector-fr-enedis")?.name = "region-connector-fr-enedis"

include("region-connectors:region-connector-es-datadis")
findProject(":region-connectors:region-connector-es-datadis")?.name = "region-connector-es-datadis"

include("region-connectors:region-connector-simulation")
findProject(":region-connectors:region-connector-simulation")?.name = "region-connector-simulation"

include("examples:example-app")
findProject(":examples:example-app")?.name = "example-app"

include("core")
findProject("core")?.name = "core"

include("outbound-kafka")

include("region-connectors:shared")
findProject(":region-connectors:shared")?.name = "shared"

