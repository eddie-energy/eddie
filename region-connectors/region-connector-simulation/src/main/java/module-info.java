import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.simulation.SimulationConnector;

module energy.eddie.regionconnector.simulation {
    requires energy.eddie.api;
    requires io.javalin;
    requires org.slf4j;
    requires reactor.core;
    requires org.eclipse.jetty.server;
    provides RegionConnector with SimulationConnector;

    // exported to give reflective access to Jackson
    exports energy.eddie.regionconnector.simulation;

    // needed requires for compilation that aren't inferred
    requires kotlin.stdlib;
}