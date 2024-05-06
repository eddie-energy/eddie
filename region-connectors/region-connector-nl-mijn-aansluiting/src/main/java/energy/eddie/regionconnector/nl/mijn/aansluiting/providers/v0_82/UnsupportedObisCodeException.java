package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

public class UnsupportedObisCodeException extends Exception {
    public UnsupportedObisCodeException(String obisCode) {
        super(obisCode);
    }
}
