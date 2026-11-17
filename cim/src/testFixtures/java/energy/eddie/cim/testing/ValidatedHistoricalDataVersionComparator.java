// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.testing;

import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Compares the semantic content of a CIM v1.04 and a CIM v1.12 validated historical data document.
 * Only used during testing to verify that both versions carry equivalent information for the same input.
 * <p>
 * Fields that are version-specific by design are not compared:
 * creation timestamps of the envelope, mRIDs and revision numbers of the market document,
 * the region country (coding scheme in v1.04, ISO country code in v1.12),
 * and the document level period, which does not exist in v1.12.
 * {@code dateAndOrTime.dateTime} is only compared if it is set in the v1.04 document,
 * since it is mandatory in v1.12, but optional in v1.04.
 */
public final class ValidatedHistoricalDataVersionComparator {
    private final List<String> differences = new ArrayList<>();

    private ValidatedHistoricalDataVersionComparator() {
    }

    /**
     * Compares the two documents.
     *
     * @param v104 the CIM v1.04 document
     * @param v112 the CIM v1.12 document
     * @return a list of human-readable differences, empty if both are equivalent
     */
    public static List<String> differences(VHDEnvelope v104, Envelope v112) {
        var comparator = new ValidatedHistoricalDataVersionComparator();
        comparator.compareEnvelope(v104, v112);
        return List.copyOf(comparator.differences);
    }

    /**
     * Compares the two documents, but ignores the given fields.
     * Useful for fields that are generated at mapping time, such as creation timestamps based on the current time.
     *
     * @param v104          the CIM v1.04 document
     * @param v112          the CIM v1.12 document
     * @param ignoredFields names of the fields to ignore, for example {@code createdDateTime} or {@code dateAndOrTime}
     * @return a list of human-readable differences, empty if both are equivalent
     */
    public static List<String> differencesIgnoring(VHDEnvelope v104, Envelope v112, String... ignoredFields) {
        return differences(v104, v112)
                .stream()
                .filter(difference -> {
                    var field = difference.substring(0, difference.indexOf(':'));
                    for (var ignored : ignoredFields) {
                        if (field.equals(ignored) || field.endsWith("." + ignored)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    private void compareEnvelope(VHDEnvelope v104, Envelope v112) {
        var meta = v112.getMessageDocumentHeader() == null
                ? null
                : v112.getMessageDocumentHeader().getMetaInformation();
        if (meta == null) {
            differences.add("v1.12 envelope has no meta information");
            return;
        }
        compare("connectionId", v104.getMessageDocumentHeaderMetaInformationConnectionId(), meta.getConnectionId());
        compare("permissionId",
                v104.getMessageDocumentHeaderMetaInformationPermissionId(),
                meta.getRequestPermissionId());
        compare("dataNeedId", v104.getMessageDocumentHeaderMetaInformationDataNeedId(), meta.getDataNeedId());
        compare("documentType", v104.getMessageDocumentHeaderMetaInformationDocumentType(), meta.getDocumentType());
        compare("regionConnector",
                v104.getMessageDocumentHeaderMetaInformationRegionConnector(),
                meta.getRegionConnector());

        var doc104 = v104.getMarketDocument();
        var doc112 = v112.getVHDMarketDocument();
        if (doc104 == null || doc112 == null) {
            compare("marketDocument present", doc104 != null, doc112 != null);
            return;
        }
        compare("createdDateTime", doc104.getCreatedDateTime(), doc112.getCreatedDateTime());
        compare("description", doc104.getDescription(), doc112.getDescription());
        compare("type", doc104.getType(), doc112.getType());
        compare("processType", doc104.getProcessProcessType(), doc112.getProcessProcessType());
        compare("sender.mRID", value(doc104.getSenderMarketParticipantMRID(), energy.eddie.cim.v1_04.vhd.PartyIDString::getValue),
                value(doc112.getSenderMarketParticipantMRID(), energy.eddie.cim.v1_12.vhd.PartyIDString::getValue));
        compare("sender.codingScheme", value(doc104.getSenderMarketParticipantMRID(), energy.eddie.cim.v1_04.vhd.PartyIDString::getCodingScheme),
                value(doc112.getSenderMarketParticipantMRID(), energy.eddie.cim.v1_12.vhd.PartyIDString::getCodingScheme));
        compare("sender.role",
                doc104.getSenderMarketParticipantMarketRoleType(),
                doc112.getSenderMarketParticipantMarketRoleType());
        compare("receiver.mRID", value(doc104.getReceiverMarketParticipantMRID(), energy.eddie.cim.v1_04.vhd.PartyIDString::getValue),
                value(doc112.getReceiverMarketParticipantMRID(), energy.eddie.cim.v1_12.vhd.PartyIDString::getValue));
        compare("receiver.codingScheme", value(doc104.getReceiverMarketParticipantMRID(), energy.eddie.cim.v1_04.vhd.PartyIDString::getCodingScheme),
                value(doc112.getReceiverMarketParticipantMRID(), energy.eddie.cim.v1_12.vhd.PartyIDString::getCodingScheme));
        compare("receiver.role",
                doc104.getReceiverMarketParticipantMarketRoleType(),
                doc112.getReceiverMarketParticipantMarketRoleType());

        var series104 = doc104.getTimeSeries();
        var series112 = doc112.getTimeSeries();
        if (series104.size() != series112.size()) {
            compare("timeSeries.size", series104.size(), series112.size());
            return;
        }
        for (int i = 0; i < series104.size(); i++) {
            compareTimeSeries("timeSeries[" + i + "]", series104.get(i), series112.get(i));
        }
    }

    @SuppressWarnings("java:S1192") // Field names are repeated for readability
    private void compareTimeSeries(
            String path,
            energy.eddie.cim.v1_04.vhd.TimeSeries ts104,
            energy.eddie.cim.v1_12.vhd.TimeSeries ts112
    ) {
        compare(path + ".version", ts104.getVersion(), ts112.getVersion());
        compare(path + ".businessType", ts104.getBusinessType(), ts112.getBusinessType());
        compare(path + ".product", ts104.getProduct(), ts112.getProduct());
        compare(path + ".flowDirection", ts104.getFlowDirectionDirection(), ts112.getFlowDirectionDirection());
        if (ts104.getDateAndOrTimeDateTime() != null) {
            compare(path + ".dateAndOrTime", ts104.getDateAndOrTimeDateTime(), ts112.getDateAndOrTimeDateTime());
        }
        compare(path + ".energyUnit", ts104.getEnergyMeasurementUnitName(), ts112.getEnergyMeasurementUnitName());
        compare(path + ".energyQualityUnit",
                ts104.getEnergyQualityMeasurementUnitName(),
                ts112.getEnergyQualityMeasurementUnitName());
        compare(path + ".reasonCode", ts104.getReasonCode(), ts112.getReasonCode());
        compare(path + ".reasonText", ts104.getReasonText(), ts112.getReasonText());
        compare(path + ".registeredResource.mRID", value(ts104.getRegisteredResourceMRID(), energy.eddie.cim.v1_04.vhd.ResourceIDString::getValue),
                value(ts112.getRegisteredResourceMRID(), energy.eddie.cim.v1_12.vhd.ResourceIDString::getValue));
        compare(path + ".registeredResource.codingScheme",
                value(ts104.getRegisteredResourceMRID(), energy.eddie.cim.v1_04.vhd.ResourceIDString::getCodingScheme),
                value(ts112.getRegisteredResourceMRID(), energy.eddie.cim.v1_12.vhd.ResourceIDString::getCodingScheme));
        compare(path + ".registeredResource.name", ts104.getRegisteredResourceName(), ts112.getRegisteredResourceName());
        compare(path + ".registeredResource.description",
                ts104.getRegisteredResourceDescription(),
                ts112.getRegisteredResourceDescription());
        compare(path + ".registeredResource.fuel",
                ts104.getRegisteredResourceFuelFuel(),
                ts112.getRegisteredResourceFuelFuel());
        compare(path + ".registeredResource.psrType",
                ts104.getRegisteredResourcePSRTypePsrType(),
                ts112.getRegisteredResourcePSRTypePsrType());
        compare(path + ".registeredResource.location.mRID",
                ts104.getRegisteredResourceLocationMRID(),
                ts112.getRegisteredResourceLocationMRID());
        compare(path + ".accountingPoint.mRID", value(ts104.getMarketEvaluationPointMRID(), energy.eddie.cim.v1_04.vhd.MeasurementPointIDString::getValue),
                value(ts112.getAccountingPointMRID(), energy.eddie.cim.v1_12.vhd.MeasurementPointIDString::getValue));
        compare(path + ".accountingPoint.codingScheme",
                value(ts104.getMarketEvaluationPointMRID(), energy.eddie.cim.v1_04.vhd.MeasurementPointIDString::getCodingScheme),
                value(ts112.getAccountingPointMRID(), energy.eddie.cim.v1_12.vhd.MeasurementPointIDString::getCodingScheme));
        compare(path + ".accountingPoint.meterReadings.mRID",
                value(ts104.getMarketEvaluationPointMeterReadingsMRID(), energy.eddie.cim.v1_04.vhd.ResourceIDString::getValue),
                value(ts112.getAccountingPointMeterReadingsMRID(), energy.eddie.cim.v1_12.vhd.ResourceIDString::getValue));
        compare(path + ".accountingPoint.meterReadings.readings.mRID",
                value(ts104.getMarketEvaluationPointMeterReadingsReadingsMRID(), energy.eddie.cim.v1_04.vhd.ResourceIDString::getValue),
                value(ts112.getAccountingPointMeterReadingsReadingsMRID(), energy.eddie.cim.v1_12.vhd.ResourceIDString::getValue));
        compare(path + ".readingType.aggregate",
                value(ts104.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(), energy.eddie.cim.v1_04.vhd.AggregateKind::value),
                value(ts112.getAccountingPointMeterReadingsReadingsReadingTypeAggregate(), energy.eddie.cim.v1_12.vhd.AggregateKind::value));
        compare(path + ".readingType.accumulation",
                value(ts104.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation(), energy.eddie.cim.v1_04.vhd.AccumulationKind::value),
                value(ts112.getAccountingPointMeterReadingsReadingsReadingTypeAccumulation(), energy.eddie.cim.v1_12.vhd.AccumulationKind::value));
        compare(path + ".readingType.commodity",
                value(ts104.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(), energy.eddie.cim.v1_04.vhd.CommodityKind::value),
                value(ts112.getAccountingPointMeterReadingsReadingsReadingTypeCommodity(), energy.eddie.cim.v1_12.vhd.CommodityKind::value));
        compare(path + ".usagePointLocation.geoInfoReference",
                ts104.getMarketEvaluationPointUsagePointLocationGeoInfoReference(),
                ts112.getAccountingPointUsagePointLocationGeoInfoReference());

        var periods104 = ts104.getPeriods();
        var periods112 = ts112.getSeriesPeriods();
        if (periods104.size() != periods112.size()) {
            compare(path + ".periods.size", periods104.size(), periods112.size());
            return;
        }
        for (int i = 0; i < periods104.size(); i++) {
            comparePeriod(path + ".periods[" + i + "]", periods104.get(i), periods112.get(i));
        }
    }

    private void comparePeriod(
            String path,
            energy.eddie.cim.v1_04.vhd.SeriesPeriod p104,
            energy.eddie.cim.v1_12.vhd.SeriesPeriod p112
    ) {
        compare(path + ".resolution", p104.getResolution(), p112.getResolution());
        compare(path + ".timeInterval.start", value(p104.getTimeInterval(), energy.eddie.cim.v1_04.vhd.ESMPDateTimeInterval::getStart),
                value(p112.getTimeInterval(), energy.eddie.cim.v1_12.vhd.ESMPDateTimeInterval::getStart));
        compare(path + ".timeInterval.end", value(p104.getTimeInterval(), energy.eddie.cim.v1_04.vhd.ESMPDateTimeInterval::getEnd),
                value(p112.getTimeInterval(), energy.eddie.cim.v1_12.vhd.ESMPDateTimeInterval::getEnd));
        compare(path + ".reasonCode", p104.getReasonCode(), p112.getReasonCode());
        compare(path + ".reasonText", p104.getReasonText(), p112.getReasonText());

        var points104 = p104.getPoints();
        var points112 = p112.getPoints();
        if (points104.size() != points112.size()) {
            compare(path + ".points.size", points104.size(), points112.size());
            return;
        }
        for (int i = 0; i < points104.size(); i++) {
            var point104 = points104.get(i);
            var point112 = points112.get(i);
            var pointPath = path + ".points[" + i + "]";
            compare(pointPath + ".position", point104.getPosition(), point112.getPosition());
            compareDecimal(pointPath + ".energyQuantity.quantity",
                           point104.getEnergyQuantityQuantity(),
                           point112.getEnergyQuantityQuantity());
            compare(pointPath + ".energyQuantity.quality",
                    point104.getEnergyQuantityQuality(),
                    point112.getEnergyQuantityQuality());
            compare(pointPath + ".energyQuantity.type",
                    value(point104.getEnergyQuantityType(), energy.eddie.cim.v1_04.vhd.QuantityTypeKind::value),
                    point112.getEnergyQuantityType());
            compareDecimal(pointPath + ".energyQualityQuantity.quantity",
                           point104.getEnergyQualityQuantityQuantity(),
                           point112.getEnergyQualityQuantityQuantity());
            compare(pointPath + ".energyQualityQuantity.quality",
                    point104.getEnergyQualityQuantityQuality(),
                    point112.getEnergyQualityQuantityQuality());
            compare(pointPath + ".energyQualityQuantity.type",
                    value(point104.getEnergyQualityQuantityType(), energy.eddie.cim.v1_04.vhd.QuantityTypeKind::value),
                    point112.getEnergyQualityQuantityType());
        }
    }

    private void compare(String field, Object v104, Object v112) {
        if (!Objects.equals(v104, v112)) {
            differences.add(field + ": v1.04=<" + v104 + "> v1.12=<" + v112 + ">");
        }
    }

    private void compareDecimal(String field, BigDecimal v104, BigDecimal v112) {
        // Compare numerically, so that different scales like 1.0 and 1.00 are treated as equal
        if (v104 == null || v112 == null || v104.compareTo(v112) != 0) {
            compare(field, v104, v112);
        }
    }

    private static <T, R> R value(T object, Function<T, R> getter) {
        return object == null ? null : getter.apply(object);
    }
}
