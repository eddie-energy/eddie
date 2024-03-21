package energy.eddie.dataneeds.persistence;

import energy.eddie.dataneeds.needs.DataNeed;

public record DataNeedsNameAndIdProjectionRecord(String id, String name) implements DataNeedsNameAndIdProjection {
    public DataNeedsNameAndIdProjectionRecord(DataNeed dataNeed) {
        this(dataNeed.id(), dataNeed.name());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
