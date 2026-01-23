// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @Test
    void testParse_withApartmentAddress() {
        Address address = Address.parse("Pseo España, 141 , 10º2b 28046-MADRID - MADRID").orElseThrow();

        assertAll(
                () -> assertEquals("Pseo España", address.street()),
                () -> assertEquals("141", address.buildingNumber()),
                () -> assertEquals("10", address.floor()),
                () -> assertEquals("2b", address.door()),
                () -> assertEquals("28046", address.postalCode()),
                () -> assertEquals("MADRID", address.city()),
                () -> assertEquals("MADRID", address.province())
        );
    }

    @Test
    void testParse_withHouseAddress() {
        Address address = Address.parse("Plaza de España, 1 , 28046-MADRID - MADRID").orElseThrow();

        assertAll(
                () -> assertEquals("Plaza de España", address.street()),
                () -> assertEquals("1", address.buildingNumber()),
                () -> assertNull(address.floor()),
                () -> assertNull(address.door()),
                () -> assertEquals("28046", address.postalCode()),
                () -> assertEquals("MADRID", address.city()),
                () -> assertEquals("MADRID", address.province())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Building , FloorºDoor Postal-City - Province",
            "Street, FloorºDoor Postal-City - Province",
            "Street, Building",
            "Street, Building , FloorºDoor Postal-City",
            "Street, Building , FloorºDoor Postal - Province",
            "Street, Building , FloorºDoor City - Province",
            "Street, Building , Province",
    })
    void testParse_withInvalidFormat(String addressString) {
        assertTrue(Address.parse(addressString).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Street, Building , FloorºDoor Postal-City - Province",
            "Street, Building , Postal-City - Province",
            "Street, Building , Door Postal-City - Province",
            "Street, Building , Floor Postal-City - Province",
    })
    void testParse_withValidFormat(String addressString) {
        assertTrue(Address.parse(addressString).isPresent());
    }
}
