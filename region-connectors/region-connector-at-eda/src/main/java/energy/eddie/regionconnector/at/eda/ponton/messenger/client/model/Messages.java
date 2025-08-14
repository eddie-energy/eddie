package energy.eddie.regionconnector.at.eda.ponton.messenger.client.model;

import java.util.List;

public record Messages(long totalResultCount, List<Message> messages) {}
