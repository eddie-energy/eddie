rootProject.name = "eddie"
include("region-connectors")
include("region-connectors:region-connector-api")
findProject(":region-connectors:region-connector-api")?.name = "region-connector-api"
include("region-connectors:region-connector-at")
findProject(":region-connectors:region-connector-at")?.name = "region-connector-at"
