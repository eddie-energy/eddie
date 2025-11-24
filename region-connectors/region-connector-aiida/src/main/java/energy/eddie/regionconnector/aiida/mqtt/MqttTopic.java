// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;
import energy.eddie.regionconnector.aiida.exceptions.MqttTopicException;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public record MqttTopic(
        String prefix,
        String permissionId,
        MqttTopicType topicType
) {
    private static final String DEFAULT_PREFIX = "aiida/v1";

    public static MqttTopic of(String permissionId, MqttTopicType topicType) {
        return new MqttTopic(DEFAULT_PREFIX, permissionId, topicType);
    }

    /**
     * Extracts the {@code permissionId} from an MQTT topic.
     *
     * <p>The expected topic format depends on whether a schema is provided:</p>
     *
     * <ul>
     *   <li><b>Without schema</b>: {@code aiida/v1/{permissionId}/{baseTopicName}}</li>
     *   <li><b>With schema</b>:    {@code aiida/v1/{permissionId}/{baseTopicName}/{schemaTopic}}</li>
     * </ul>
     *
     * <p>The {@code baseTopicName} is derived from the given {@link MqttTopicType},
     * and the optional schema suffix is taken from {@link AiidaSchema#topicName()}.</p>
     *
     * @param topicStr  the MQTT topic string to parse
     * @param topicType the expected topic type (defines the base topic segment)
     * @param schema    the schema used for this topic, or {@code null} if no schema suffix is expected
     * @return the extracted permissionId
     * @throws MqttTopicException if the topic does not conform to the expected pattern
     */
    public static String extractPermissionIdFromTopic(
            String topicStr,
            MqttTopicType topicType,
            @Nullable AiidaSchema schema
    ) throws MqttTopicException {
        var pattern = buildTopicPattern(topicType, schema);
        var matcher = pattern.matcher(topicStr);

        if (matcher.matches() && matcher.groupCount() >= 1) {
            return matcher.group(1);
        } else {
            throw new MqttTopicException("Topic does not match expected pattern: " + pattern);
        }
    }

    public String eddieTopic() {
        return buildTopic(MqttAction.PUBLISH);
    }

    public String aiidaTopic() {
        return buildTopic(MqttAction.SUBSCRIBE);
    }

    public String baseTopic() {
        return String.join("/", prefix, permissionId, topicType.baseTopicName());
    }

    public MqttAcl aiidaAcl(String username) {
        return new MqttAcl(
                username,
                topicType.aiidaAclAction(),
                topicType.aiidaAclType(),
                baseTopic() + topicType.topicSuffix()
        );
    }

    /**
     * Builds a regex pattern to match MQTT topics for the given topic type and schema.
     * The resulting pattern captures exactly one named group: {@code permissionId}.
     *
     * <p>The generated regex matches topics with the following structure:</p>
     *
     * <ul>
     *   <li>{@code aiida/v1/{permissionId}/{baseTopicName}} — if {@code schema} is {@code null}</li>
     *   <li>{@code aiida/v1/{permissionId}/{baseTopicName}/{schemaTopic}} — if a schema is provided</li>
     * </ul>
     *
     * <p>The pattern is anchored ({@code ^...$}) to enforce full-string matching and prevent substring matches.</p>
     *
     * @param topicType the MQTT topic type determining the {@code baseTopicName}
     * @param schema    optional schema that adds a trailing path segment; may be {@code null}
     * @return a compiled {@link Pattern} capable of extracting the {@code permissionId} from a topic
     */
    private static Pattern buildTopicPattern(MqttTopicType topicType, @Nullable AiidaSchema schema) {
        var base = Pattern.quote(topicType.baseTopicName());
        var prefix = Pattern.quote(DEFAULT_PREFIX);

        var schemaTopic = schema != null
                ? "/" + Pattern.quote(schema.topicName())
                : "";

        var regex = "^" + prefix + "/(?<permissionId>[^/]+)/" + base + schemaTopic + "$";
        return Pattern.compile(regex);
    }

    private String buildTopic(MqttAction requiredAction) {
        var suffix = (topicType.aiidaAclAction() == requiredAction)
                ? topicType.topicSuffix()
                : "";

        return baseTopic() + suffix;
    }
}
