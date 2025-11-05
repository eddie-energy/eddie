package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import energy.eddie.regionconnector.aiida.exceptions.MqttTopicException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MqttTopic(
        String prefix,
        String permissionId,
        MqttTopicType topicType
) {
    private static final String DEFAULT_PREFIX = "aiida/v1";
    private static final String SUFFIX_WILDCARD = "#";

    public static MqttTopic of(String permissionId, MqttTopicType topicType) {
        return new MqttTopic(DEFAULT_PREFIX, permissionId, topicType);
    }

    public static String extractPermissionIdFromTopic(
            String topicStr,
            MqttTopicType topicType,
            AiidaSchema schema
    ) throws MqttTopicException {
        var regex = String.format(
                "^%s/(.+?)/%s(/%s)?$",
                DEFAULT_PREFIX,
                Pattern.quote(topicType.topicName()),
                Pattern.quote(schema.topicName())
        );

        Matcher matcher = Pattern.compile(regex).matcher(topicStr);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            return matcher.group(1);
        } else {
            throw new MqttTopicException("Topic does not match expected pattern: " + regex);
        }
    }

    public String topicPattern() {
        return baseTopic() + (topicType.hasSuffix() ? "/" + SUFFIX_WILDCARD : "");
    }

    public String baseTopic() {
        return String.join("/", prefix, permissionId, topicType.topicName());
    }

    public MqttAcl aiidaAcl(String username) {
        return new MqttAcl(
                username,
                topicType.aiidaAclAction(),
                topicType.aiidaAclType(),
                topicPattern()
        );
    }
}
