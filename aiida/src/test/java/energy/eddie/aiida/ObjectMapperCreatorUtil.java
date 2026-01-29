package energy.eddie.aiida;

import energy.eddie.aiida.config.AiidaConfiguration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ObjectMapperCreatorUtil {
    public static ObjectMapper mapper() {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        return builder.build();
    }
}
