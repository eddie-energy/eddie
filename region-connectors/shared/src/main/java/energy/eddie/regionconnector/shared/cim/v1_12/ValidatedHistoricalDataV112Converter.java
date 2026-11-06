// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12;

import org.jspecify.annotations.Nullable;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.*;

/** Converts existing historical measurements without requesting or manufacturing provider data. */
public final class ValidatedHistoricalDataV112Converter {
    private ValidatedHistoricalDataV112Converter() { }

    public static Envelope convert(VHDEnvelope source) {
        var meta = new MetaInformation()
                .withConnectionId(source.getMessageDocumentHeaderMetaInformationConnectionId())
                .withRequestPermissionId(source.getMessageDocumentHeaderMetaInformationPermissionId())
                .withDataNeedId(source.getMessageDocumentHeaderMetaInformationDataNeedId())
                .withDocumentType(source.getMessageDocumentHeaderMetaInformationDocumentType())
                .withRegionConnector(source.getMessageDocumentHeaderMetaInformationRegionConnector())
                .withRegionCountry(source.getMessageDocumentHeaderMetaInformationRegionCountry());
        var document = convert(source.getMarketDocument());
        if (document != null) {
            for (var series : document.getTimeSeries()) {
                if (series.getDateAndOrTimeDateTime() == null) {
                    // Required in 1.12: EDDIE message creation time when no provider process time exists.
                    series.setDateAndOrTimeDateTime(source.getMarketDocument().getCreatedDateTime());
                }
            }
        }
        return new Envelope()
                .withMessageDocumentHeader(new MessageDocumentHeader()
                        .withCreationDateTime(source.getMessageDocumentHeaderCreationDateTime())
                        .withMetaInformation(meta))
                .withVHDMarketDocument(document);
    }

    private static @Nullable Point convert(energy.eddie.cim.v1_04.vhd.@Nullable Point source) {
        if (source == null) {
            return null;
        }
        var target = new Point();
        target.setPosition(source.getPosition());
        target.setEnergyQuantityQuantity(source.getEnergyQuantityQuantity());
        target.setEnergyQuantityType(source.getEnergyQuantityType() == null ? null : source.getEnergyQuantityType().value());
        target.setEnergyQuantityQuality(source.getEnergyQuantityQuality());
        target.setEnergyQualityQuantityQuantity(source.getEnergyQualityQuantityQuantity());
        target.setEnergyQualityQuantityType(source.getEnergyQualityQuantityType() == null ? null : source.getEnergyQualityQuantityType().value());
        target.setEnergyQualityQuantityQuality(source.getEnergyQualityQuantityQuality());
        return target;
    }

    private static @Nullable ESMPDateTimeInterval convert(energy.eddie.cim.v1_04.vhd.@Nullable ESMPDateTimeInterval source) {
        if (source == null) {
            return null;
        }
        var target = new ESMPDateTimeInterval();
        target.setStart(source.getStart());
        target.setEnd(source.getEnd());
        return target;
    }

    private static @Nullable SeriesPeriod convert(energy.eddie.cim.v1_04.vhd.@Nullable SeriesPeriod source) {
        if (source == null) {
            return null;
        }
        var target = new SeriesPeriod();
        target.setResolution(source.getResolution());
        target.setTimeInterval(convert(source.getTimeInterval()));
        target.getPoints().addAll(source.getPoints().stream().map(ValidatedHistoricalDataV112Converter::convert).toList());
        target.setReasonCode(source.getReasonCode());
        target.setReasonText(source.getReasonText());
        return target;
    }

    private static @Nullable ResourceIDString convert(energy.eddie.cim.v1_04.vhd.@Nullable ResourceIDString source) {
        if (source == null) {
            return null;
        }
        var target = new ResourceIDString();
        target.setValue(source.getValue());
        target.setCodingScheme(source.getCodingScheme());
        return target;
    }

    private static @Nullable MeasurementPointIDString convert(energy.eddie.cim.v1_04.vhd.@Nullable MeasurementPointIDString source) {
        if (source == null) {
            return null;
        }
        var target = new MeasurementPointIDString();
        target.setValue(source.getValue());
        target.setCodingScheme(source.getCodingScheme());
        return target;
    }

    private static @Nullable PartyIDString convert(energy.eddie.cim.v1_04.vhd.@Nullable PartyIDString source) {
        if (source == null) {
            return null;
        }
        var target = new PartyIDString();
        target.setValue(source.getValue());
        target.setCodingScheme(source.getCodingScheme());
        return target;
    }

    private static @Nullable TimeSeries convert(energy.eddie.cim.v1_04.vhd.@Nullable TimeSeries source) {
        if (source == null) {
            return null;
        }
        var target = new TimeSeries();
        target.setVersion(source.getVersion());
        target.setMRID(source.getMRID());
        target.setBusinessType(source.getBusinessType());
        target.setProduct(source.getProduct());
        target.setEnergyMeasurementUnitName(source.getEnergyMeasurementUnitName());
        target.setFlowDirectionDirection(source.getFlowDirectionDirection());
        target.getSeriesPeriods().addAll(source.getPeriods().stream().map(ValidatedHistoricalDataV112Converter::convert).toList());
        target.setRegisteredResourceMRID(convert(source.getRegisteredResourceMRID()));
        target.setRegisteredResourceName(source.getRegisteredResourceName());
        target.setRegisteredResourceDescription(source.getRegisteredResourceDescription());
        target.setRegisteredResourceFuelFuel(source.getRegisteredResourceFuelFuel());
        target.setRegisteredResourceLocationMRID(source.getRegisteredResourceLocationMRID());
        target.setRegisteredResourceLocationGeometryCoordinateSystemCrsUrn(source.getRegisteredResourceLocationCoordinateSystemCrsUrn());
        target.setRegisteredResourceLocationGeometryPositionPointXPosition(source.getRegisteredResourceLocationPositionPointsXPosition());
        target.setRegisteredResourceLocationGeometryPositionPointYPosition(source.getRegisteredResourceLocationPositionPointsYPosition());
        target.setRegisteredResourceLocationGeometryPositionPointZPosition(source.getRegisteredResourceLocationPositionPointsZPosition());
        target.setRegisteredResourcePSRTypePsrType(source.getRegisteredResourcePSRTypePsrType());
        target.setAccountingPointMRID(convert(source.getMarketEvaluationPointMRID()));
        target.setAccountingPointMeterReadingsMRID(convert(source.getMarketEvaluationPointMeterReadingsMRID()));
        target.setAccountingPointMeterReadingsReadingsMRID(convert(source.getMarketEvaluationPointMeterReadingsReadingsMRID()));
        target.setAccountingPointMeterReadingsReadingsReadingTypeAccumulation(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation() == null ? null : AccumulationKind.fromValue(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation().value()));
        target.setAccountingPointMeterReadingsReadingsReadingTypeAggregate(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate() == null ? null : AggregateKind.fromValue(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate().value()));
        target.setAccountingPointMeterReadingsReadingsReadingTypeCommodity(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity() == null ? null : CommodityKind.fromValue(source.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity().value()));
        target.setAccountingPointUsagePointLocationGeoInfoReference(source.getMarketEvaluationPointUsagePointLocationGeoInfoReference());
        target.setReasonCode(source.getReasonCode());
        target.setReasonText(source.getReasonText());
        target.setEnergyQualityMeasurementUnitName(source.getEnergyQualityMeasurementUnitName());
        target.setDateAndOrTimeDateTime(source.getDateAndOrTimeDateTime());
        return target;
    }

    private static @Nullable VHDMarketDocument convert(energy.eddie.cim.v1_04.vhd.@Nullable VHDMarketDocument source) {
        if (source == null) {
            return null;
        }
        var target = new VHDMarketDocument();
        target.setMRID(source.getMRID());
        target.setRevisionNumber(source.getRevisionNumber());
        target.setDescription(source.getDescription());
        target.setType(source.getType());
        target.setCreatedDateTime(source.getCreatedDateTime());
        target.setSenderMarketParticipantMRID(convert(source.getSenderMarketParticipantMRID()));
        target.setSenderMarketParticipantMarketRoleType(source.getSenderMarketParticipantMarketRoleType());
        target.setReceiverMarketParticipantMRID(convert(source.getReceiverMarketParticipantMRID()));
        target.setReceiverMarketParticipantMarketRoleType(source.getReceiverMarketParticipantMarketRoleType());
        target.setProcessProcessType(source.getProcessProcessType());
        target.getTimeSeries().addAll(source.getTimeSeries().stream().map(ValidatedHistoricalDataV112Converter::convert).toList());
        return target;
    }
}
