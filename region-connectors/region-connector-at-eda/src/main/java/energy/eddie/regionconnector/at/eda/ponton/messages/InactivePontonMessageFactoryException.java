package energy.eddie.regionconnector.at.eda.ponton.messages;

public class InactivePontonMessageFactoryException extends Throwable {
    private final Class<?> classType;

    public InactivePontonMessageFactoryException(Class<?> classType) {this.classType = classType;}

    @Override
    public String getMessage() {
        return classType.getName() + " is not active anymore and should be removed";
    }
}
