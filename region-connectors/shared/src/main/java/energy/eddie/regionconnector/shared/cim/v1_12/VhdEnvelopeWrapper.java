// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.cim.v1_12.vhd.MessageDocumentHeader;
import energy.eddie.cim.v1_12.vhd.MetaInformation;
import energy.eddie.cim.v1_12.vhd.VHDMarketDocument;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.shared.cim.v0_82.CimUtils.getCodingSchemeVhdV104;

public final class VhdEnvelopeWrapper {
    private final @Nullable VHDMarketDocument document;
    private final PermissionRequest permissionRequest;
    private final ZonedDateTime created;

    public VhdEnvelopeWrapper(
            @Nullable VHDMarketDocument document,
            PermissionRequest permissionRequest,
            ZonedDateTime created
    ) {
        this.document = document;
        this.permissionRequest = permissionRequest;
        this.created = created;
    }

    public Envelope wrap() {
        ensureProcessTimestamps();
        return new Envelope()
                .withVHDMarketDocument(document)
                .withMessageDocumentHeader(new MessageDocumentHeader()
                        .withCreationDateTime(created)
                        .withMetaInformation(new MetaInformation()
                                .withDocumentType(DocumentType.VALIDATED_HISTORICAL_DATA_MARKET_DOCUMENT.description())
                                .withRegionConnector(permissionRequest.dataSourceInformation().regionConnectorId())
                                .withDataNeedId(permissionRequest.dataNeedId())
                                .withConnectionId(permissionRequest.connectionId())
                                .withRequestPermissionId(permissionRequest.permissionId())
                                .withRegionCountry(getCodingSchemeVhdV104(permissionRequest.dataSourceInformation().countryCode()))));
    }

    /**
     * CIM 1.12 requires {@code dateAndOrTime.dateTime} on every time series, which CIM 1.04 does not provide.
     * Missing values fall back to the document creation time, or to the envelope creation time if the document has none.
     * This updates the given document in place. Because the fallback for a given document is always the same,
     * wrapping one document for several permission requests yields identical results.
     */
    private void ensureProcessTimestamps() {
        if (document == null) {
            return;
        }
        var fallback = document.getCreatedDateTime() != null ? document.getCreatedDateTime() : created;
        for (var series : document.getTimeSeries()) {
            if (series.getDateAndOrTimeDateTime() == null) {
                series.setDateAndOrTimeDateTime(fallback);
            }
        }
    }
}
