package energy.eddie.regionconnector.simulation.engine.steps;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

public final class Scenario extends Model {
    public static final String DISCRIMINATOR_VALUE = "Scenario";
    private static final Logger LOGGER = LoggerFactory.getLogger(Scenario.class);
    private final String name;
    private final List<Model> steps;

    public Scenario(String name, List<Model> steps) {
        super(DISCRIMINATOR_VALUE);
        this.name = name;
        this.steps = steps;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        LOGGER.debug("Starting scenario {}", name);
        return List.copyOf(steps);
    }

    @JsonProperty
    public String name() {return name;}

    @JsonProperty
    public List<Model> steps() {return steps;}

    @Override
    public int hashCode() {
        return Objects.hash(name, steps);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Scenario) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.steps, that.steps);
    }

    @Override
    public String toString() {
        return "Scenario[" +
               "name=" + name + ", " +
               "steps=" + steps + ']';
    }
}
