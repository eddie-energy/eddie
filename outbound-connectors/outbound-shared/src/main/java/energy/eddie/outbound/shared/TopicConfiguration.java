package energy.eddie.outbound.shared;

public class TopicConfiguration {
    private final String eddieId;

    public TopicConfiguration(String eddieId) {this.eddieId = eddieId;}

    public String toTopic(TopicStructure.Direction direction, TopicStructure.DataModels dataModel, TopicStructure.DocumentTypes documentType) {
        return TopicStructure.toTopic(direction, eddieId, dataModel, documentType);
    }
    public String toEddieProductionTopic(TopicStructure.DataModels dataModel, TopicStructure.DocumentTypes documentType) {
        return toTopic(TopicStructure.Direction.EP, dataModel, documentType);
    }
}
