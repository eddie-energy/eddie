package at.eda.config;

import java.time.ZoneId;

public interface AtConfiguration {
    String  eligiblePartyId();

    ZoneId timeZone();
}
