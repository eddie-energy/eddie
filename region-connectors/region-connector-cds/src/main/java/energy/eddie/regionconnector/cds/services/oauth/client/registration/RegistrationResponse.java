package energy.eddie.regionconnector.cds.services.oauth.client.registration;

public sealed interface RegistrationResponse permits RegistrationResponse.Registered, RegistrationResponse.RegistrationError {
    record RegistrationError(String description) implements RegistrationResponse {}
    record Registered(String clientId, String clientSecret) implements RegistrationResponse {}
}
