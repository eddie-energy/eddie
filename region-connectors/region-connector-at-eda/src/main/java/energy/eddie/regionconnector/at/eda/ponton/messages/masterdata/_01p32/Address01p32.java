package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

import at.ebutilities.schemata.customerprocesses.masterdata._01p32.StreetC;
import energy.eddie.regionconnector.at.eda.dto.masterdata.Address;
import jakarta.annotation.Nullable;

public record Address01p32(
        at.ebutilities.schemata.customerprocesses.masterdata._01p32.Address address) implements Address {
    @Nullable
    @Override
    public String zipCode() {
        var zip = address.getZIP();
        return zip == null ? null : zip.getValue();
    }

    @Nullable
    @Override
    public String city() {
        var city = address.getCity();
        return city == null ? null : city.getValue();
    }

    @Nullable
    @Override
    public String street() {
        StreetC street = address.getStreet();
        return street == null ? null : street.getValue();
    }

    @Nullable
    @Override
    public String streetNumber() {
        var streetNo = address.getStreetNo();
        return streetNo == null ? null : streetNo.getValue();
    }

    @Nullable
    @Override
    public String staircase() {
        var staircase = address.getStaircase();
        return staircase == null ? null : staircase.getValue();
    }

    @Nullable
    @Override
    public String floor() {
        var floor = address.getFloor();
        return floor == null ? null : floor.getValue();
    }

    @Nullable
    @Override
    public String door() {
        var door = address.getDoorNumber();
        return door == null ? null : door.getValue();
    }
}
