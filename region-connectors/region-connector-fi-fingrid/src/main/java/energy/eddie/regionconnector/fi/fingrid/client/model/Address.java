// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.client.model;

public interface Address {
    String addressNote();

    String apartment();

    String buildingNumber();

    String postalCode();

    String stairwellIdentification();

    String streetName();

    String postOffice();
}
