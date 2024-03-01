package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;

import java.util.List;
import java.util.Objects;

import static energy.eddie.regionconnector.at.eda.requests.CCMORequest.DSO_ID_LENGTH;

public class MeteringPointMatchesDsoIdValidator implements Validator<CreatedEvent> {
    private boolean meteringPointMatchesDsoId(CreatedEvent value) {
        return Objects.equals(
                value.dataSourceInformation().meteredDataAdministratorId(),
                value.meteringPointId().substring(0, DSO_ID_LENGTH)
        );
    }

    @Override
    public List<AttributeError> validate(CreatedEvent value) {

        if (value.meteringPointId() == null
                || value.meteringPointId().isBlank()
                || meteringPointMatchesDsoId(value)) {
            return List.of();
        }
        return List.of(
                new AttributeError("meteringPointId", "The dsoId does not match the dsoId of the metering point")
        );
    }
}