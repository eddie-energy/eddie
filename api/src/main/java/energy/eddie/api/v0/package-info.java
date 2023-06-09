/**
 * <h2>EDDIE API version 0</h2>
 * <p><em>preliminary version</em>
 *
 * <p>
 * This package defines the API needed for writing Plugins for the EDDIE frameworks. These kinds of
 * plugins are supported:
 *
 * <ul>
 *     <li><em>Region connectors (interface {@link energy.eddie.api.v0.RegionConnector}):</em> These enable receiving
 *     data from metered data administrators after the user of the eligible party (EP) application gave her/his consent
 *     to the EP app. They implement the consent process needed by the associated consent administrator portal as well
 *     as transforming the MDA specific data structures into the EDDIE CIM format.
 *     </li>
 *     <li><em>Application connectors (interface {@link energy.eddie.api.v0.ApplicationConnector}:</em> These provide
 *     transports mechanisms to deliver metering data to the EP applications, e.g. via a Kafka Topic or maybe written
 *     directly into a db.</li>
 * </ul>
 * <p>
 * Besides the plugin interfaces the data types needed for them are also defined in this package. The most important one
 * is the class {@link energy.eddie.api.v0.ConsumptionRecord} which defines energy consumption data in a CIM compliant
 * data format. These data types are defined as a JSON schema in <code>src/main/schema.json</code> and the Java classes
 * generated during build.
 */
package energy.eddie.api.v0;
