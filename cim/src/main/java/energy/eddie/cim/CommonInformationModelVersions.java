// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim;

/**
 * The supported versions of the CIM
 */
public enum CommonInformationModelVersions {
    /**
     * Version 0.82 of the CIM.
     * This version is mostly the &quot;EDDIE&quot; flavor of the CIM.
     * Therefore, it is not completely CIM compliant.
     */
    V0_82("0.82"),
    /**
     * Version 0.91.08 of the CIM.
     * Only the {@link energy.eddie.cim.v0_91_08.RTREnvelope} market document is supported in this version.
     */
    V0_91_08("0.91.08"),
    /**
     * Version 1.04 of the CIM.
     */
    V1_04("1.04"),
    /**
     * Version 1.12 of the CIM.
     */
    V1_12("1.12");

    private final String version;

    CommonInformationModelVersions(String version) {
        this.version = version;
    }

    /**
     * Returns the version String of the CIM version.
     *
     * @return the version String of the CIM version.
     */
    public String version() {
        return version;
    }

    /**
     * Returns the version String without the periods, since the CIM does not allow periods in the version String of the final XML documents.
     *
     * @return the version String without the periods.
     */
    public String cimify() {
        return version.replace(".", "");
    }
}
