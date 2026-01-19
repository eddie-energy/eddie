package energy.eddie.aiida.models.datasource.mqtt.it;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.errors.datasource.mqtt.it.SinapsiAlflaEmptyConfigException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.MqttUser;
import energy.eddie.aiida.services.secrets.SecretType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

import static energy.eddie.aiida.services.secrets.KeyStoreSecretsService.alias;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SINAPSI_ALFA)
public class SinapsiAlfaDataSource extends MqttDataSource {
    @Transient
    @JsonIgnore
    private String activationKey;

    @Transient
    @JsonIgnore
    private SinapsiAlfaConfiguration config;

    @SuppressWarnings("NullAway")
    protected SinapsiAlfaDataSource() {}

    @SuppressWarnings("NullAway")
    public SinapsiAlfaDataSource(SinapsiAlfaDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    private SinapsiAlfaDataSource(SinapsiAlfaDataSource sinapsiAlfaDataSource, String alias) {
        super(sinapsiAlfaDataSource);
        internalHost = sinapsiAlfaDataSource.internalHost;
        externalHost = sinapsiAlfaDataSource.externalHost;
        activationKey = sinapsiAlfaDataSource.activationKey;
        config = sinapsiAlfaDataSource.config;
        user = sinapsiAlfaDataSource.user.copyWithAliasAsPassword(alias);
    }

    public void configure(
            SinapsiAlfaConfiguration config,
            String activationKey
    ) throws SinapsiAlflaEmptyConfigException {
        if (config.mqttUsername().isEmpty() || config.mqttPassword().isEmpty()) {
            throw new SinapsiAlflaEmptyConfigException();
        }

        this.internalHost = config.mqttHost();
        this.externalHost = config.mqttHost();
        this.activationKey = activationKey;
        this.config = config;
    }

    @Override
    public void updatePassword(BCryptPasswordEncoder encoder, String plaintextPassword) {
        // Ignore, as the password is set in the constructor
    }

    public SinapsiAlfaDataSource copyWithAliasAsPassword() {
        return new SinapsiAlfaDataSource(this, alias(id, SecretType.PASSWORD));
    }

    @Override
    protected void createAccessControlEntry() {
        var username = config.mqttUsername();
        var topic = SinapsiAlfaConfiguration.TOPIC_PREFIX
                    + username
                    + SinapsiAlfaConfiguration.TOPIC_INFIX
                    + activationKey
                    + SinapsiAlfaConfiguration.TOPIC_SUFFIX;

        this.accessControlEntry = new MqttAccessControlEntry(username, topic);
    }

    @Override
    protected void createMqttUser() {
        this.user = new MqttUser(config.mqttUsername(), alias(id, SecretType.PASSWORD));
    }
}
