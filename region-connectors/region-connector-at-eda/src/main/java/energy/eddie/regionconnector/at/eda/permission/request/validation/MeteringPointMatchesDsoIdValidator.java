package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;

import java.util.List;
import java.util.Objects;

import static energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint.DSO_ID_LENGTH;

public class MeteringPointMatchesDsoIdValidator implements Validator<CreatedEvent> {
    @Override
    public List<AttributeError> validate(CreatedEvent value) {

        var meteringPointId = value.meteringPointId();
        var dsoId = value.dataSourceInformation().meteredDataAdministratorId();

        if (meteringPointId == null
            || meteringPointId.isBlank()
            || meteringPointMatchesDsoId(meteringPointId, dsoId)) {
            return List.of();
        }
        return List.of(
                new AttributeError("meteringPointId", "The dsoId does not match the dsoId of the metering point")
        );
    }

    private boolean meteringPointMatchesDsoId(String meteringPointId, String dsoId) {
        return Objects.equals(dsoId, meteringPointId.substring(0, DSO_ID_LENGTH));
    }
}
