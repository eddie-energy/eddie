import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.simulation.SimulationRegionConnectorFactory;

module energy.eddie.regionconnector.simulation {
    requires energy.eddie.api;
    requires io.javalin;
    requires org.slf4j;
    requires reactor.core;
    requires org.eclipse.jetty.server;
    provides RegionConnectorFactory with SimulationRegionConnectorFactory;

    // exported to give reflective access to Jackson
    exports energy.eddie.regionconnector.simulation;

    // needed requires for compilation that aren't inferred
    requires kotlin.stdlib;
    requires eclipse.microprofile.config.api;

}