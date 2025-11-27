// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim;

/**
 * The supported versions of the CIM
 */
public enum CommonInformationModelVersions {
    V0_82("0.82"),
    V0_91_08("0.91.08"),
    V1_04("1.04"),
    V1_06("1.06");

    private final String version;

    CommonInformationModelVersions(String version) {
        this.version = version;
    }

    public String version() {
        return version;
    }

    public String cimify() {
        return version.replace(".", "");
    }
}
