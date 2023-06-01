package energy.eddie.api.v0;

/**
 * Metadata describing a {@link RegionConnector}.
 * @param mdaCode short name of a region connectar that is also used as an URL fragment
 * @param mdaDisplayName name of the region connector as displayed in the  UI
 * @param countryCode country code for the supported MDA/MDAs
 * @param urlPath url base path ending with a slash '/', all acessible HTTP resources of this {@link RegionConnector}
 *                are acessible under this root-relative URL, e.g. `/region-connectors/at/eda`
 * @param coveredMeteringPoints number of metering points that are accessible through a {@link RegionConnector}
 */
public record RegionConnectorMetadata(
        String mdaCode,
        String mdaDisplayName,
        String countryCode,
        String urlPath,
        long coveredMeteringPoints
) {
}
