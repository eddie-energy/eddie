rootProject.name = "eddie"
include("region-connectors")

include("api")
findProject("api")?.name = "api"

include("region-connectors:region-connector-api")
findProject(":region-connectors:region-connector-api")?.name = "region-connector-api"

include("region-connectors:region-connector-at")
findProject(":region-connectors:region-connector-at")?.name = "region-connector-at"

include("region-connectors:region-connector-fr-enedis")
findProject(":region-connectors:region-connector-fr-enedis")?.name = "region-connector-fr-enedis"

include("framework")
findProject("framework")?.name = "framework"
