-- Create the main table for RegionConnectorFilter
CREATE TABLE region_connector_filter
(
    data_need_id VARCHAR(255) PRIMARY KEY,
    type         VARCHAR(255) NOT NULL
);

-- Create the table for region_connectors which is an ElementCollection
CREATE TABLE region_connector_filter_ids
(
    data_need_id VARCHAR(255),
    rc_id        VARCHAR(255),
    FOREIGN KEY (data_need_id) REFERENCES region_connector_filter (data_need_id) ON DELETE CASCADE
);
