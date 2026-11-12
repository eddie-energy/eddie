// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12.vhd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.cim.v1_12.vhd.MessageDocumentHeader;
import energy.eddie.cim.v1_12.vhd.MetaInformation;
import energy.eddie.cim.v1_12.vhd.VHDMarketDocument;
import energy.eddie.regionconnector.shared.cim.v1_12.DocumentType;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Wraps a CIM v1.12 validated historical data market document into its envelope.
 * The envelope header is derived only from the permission request, so it is independent of other CIM versions.
 * The region country is the upper-case ISO country code of the data source.
 */
public class VhdEnvelopeWrapper {
    private final VHDMarketDocument vhdMarketDocument;
    private final PermissionRequest permissionRequest;
    private final Clock clock;

    public VhdEnvelopeWrapper(VHDMarketDocument vhdMarketDocument, PermissionRequest permissionRequest) {
        this(vhdMarketDocument, permissionRequest, Clock.systemUTC());
    }

    public VhdEnvelopeWrapper(VHDMarketDocument vhdMarketDocument, PermissionRequest permissionRequest, Clock clock) {
        this.vhdMarketDocument = vhdMarketDocument;
        this.permissionRequest = permissionRequest;
        this.clock = clock;
    }

    public Envelope wrap() {
        var dataSourceInformation = permissionRequest.dataSourceInformation();
        return new Envelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withCreationDateTime(ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC))
                                .withMetaInformation(
                                        new MetaInformation()
                                                .withConnectionId(permissionRequest.connectionId())
                                                .withRequestPermissionId(permissionRequest.permissionId())
                                                .withDataNeedId(permissionRequest.dataNeedId())
                                                .withDocumentType(DocumentType.VALIDATED_HISTORICAL_DATA_MARKET_DOCUMENT.description())
                                                .withRegionConnector(dataSourceInformation.regionConnectorId())
                                                .withRegionCountry(dataSourceInformation.countryCode().toUpperCase(Locale.ROOT))
                                )
                )
                .withVHDMarketDocument(vhdMarketDocument);
    }
}
