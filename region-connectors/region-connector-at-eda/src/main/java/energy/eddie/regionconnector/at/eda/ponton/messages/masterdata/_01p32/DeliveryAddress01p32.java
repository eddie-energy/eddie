// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

import at.ebutilities.schemata.customerprocesses.masterdata._01p32.StreetC;
import energy.eddie.regionconnector.at.eda.dto.masterdata.DeliveryAddress;
import jakarta.annotation.Nullable;

public record DeliveryAddress01p32(
        at.ebutilities.schemata.customerprocesses.masterdata._01p32.DeliveryAddress deliveryAddress) implements DeliveryAddress {
    @Nullable
    @Override
    public String zipCode() {
        var zip = deliveryAddress.getZIP();
        return zip == null ? null : zip.getValue();
    }

    @Nullable
    @Override
    public String city() {
        var city = deliveryAddress.getCity();
        return city == null ? null : city.getValue();
    }

    @Nullable
    @Override
    public String street() {
        StreetC street = deliveryAddress.getStreet();
        return street == null ? null : street.getValue();
    }

    @Nullable
    @Override
    public String streetNumber() {
        var streetNo = deliveryAddress.getStreetNo();
        return streetNo == null ? null : streetNo.getValue();
    }

    @Nullable
    @Override
    public String staircase() {
        var staircase = deliveryAddress.getStaircase();
        return staircase == null ? null : staircase.getValue();
    }

    @Nullable
    @Override
    public String floor() {
        var floor = deliveryAddress.getFloor();
        return floor == null ? null : floor.getValue();
    }

    @Nullable
    @Override
    public String door() {
        var door = deliveryAddress.getDoorNumber();
        return door == null ? null : door.getValue();
    }

    @Nullable
    @Override
    public String addressAddition() {
        var addressAddition = deliveryAddress.getDeliveryAddressData();
        return addressAddition == null ? null : addressAddition.getValue();
    }
}
