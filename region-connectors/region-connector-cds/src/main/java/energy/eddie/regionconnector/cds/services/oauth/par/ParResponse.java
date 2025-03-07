package energy.eddie.regionconnector.cds.services.oauth.par;

public sealed interface ParResponse permits ErrorParResponse, SuccessfulParResponse, UnableToSendPar {
}
