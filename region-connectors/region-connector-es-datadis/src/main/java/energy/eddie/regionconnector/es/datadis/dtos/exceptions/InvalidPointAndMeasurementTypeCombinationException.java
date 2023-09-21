package energy.eddie.regionconnector.es.datadis.dtos.exceptions;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;

public class InvalidPointAndMeasurementTypeCombinationException extends Exception {

    public InvalidPointAndMeasurementTypeCombinationException(Integer pointType, MeasurementType measurementType) {
        super("Point type " + pointType + " does not support measurement type " + measurementType);
    }
}