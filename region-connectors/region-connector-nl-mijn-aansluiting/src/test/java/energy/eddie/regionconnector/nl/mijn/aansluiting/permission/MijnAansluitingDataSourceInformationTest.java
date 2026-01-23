// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MijnAansluitingDataSourceInformationTest {
    @Test
    void testHashCode() {
        // Given, When, Then
        assertEquals(new MijnAansluitingDataSourceInformation().hashCode(),
                     new MijnAansluitingDataSourceInformation().hashCode());
    }
}