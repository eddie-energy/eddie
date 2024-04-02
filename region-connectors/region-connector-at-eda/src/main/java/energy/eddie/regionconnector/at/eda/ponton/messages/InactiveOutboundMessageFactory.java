package energy.eddie.regionconnector.at.eda.ponton.messages;

public class InactiveOutboundMessageFactory extends Throwable {
    private final Class<?> classType;

    public InactiveOutboundMessageFactory(Class<?> classType) {this.classType = classType;}

    @Override
    public String getMessage() {
        return classType.getName() + " is not active anymore and should be removed";
    }
}
