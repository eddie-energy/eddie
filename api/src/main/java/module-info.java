/**
 * <h2>EDDIE inter-module API definitions</h2>
 *
 * <p>This module defines the APIs that are used between the different components of EDDIE. Each
 * component is delivered as a JPMS modules. This covers currently region connectors and application connectors.
 *
 * <p>Because the interfaces will evolve over time, there is a versioning scheme in the package name. This enables the
 * possibility of having plugins using different versions of the same API.
 *
 * @see energy.eddie.api.v0
 */
module energy.eddie.api {
    exports energy.eddie.api.v0;
    exports energy.eddie.api.v0.process.model;
    exports energy.eddie.api.v0.process.model.states;
    exports energy.eddie.cim.validated_historical_data.v0_82;
    exports energy.eddie.api.v0_82.cim;
    exports energy.eddie.api.v0_82.cim.config;
    exports energy.eddie.api.v0_82;

    requires com.fasterxml.jackson.annotation;
    requires java.compiler;
    requires eclipse.microprofile.config.api;
    requires jakarta.annotation;
    requires jakarta.xml.bind;
}