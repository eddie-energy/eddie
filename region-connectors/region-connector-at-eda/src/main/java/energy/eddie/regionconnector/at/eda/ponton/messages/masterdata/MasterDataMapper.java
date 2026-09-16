// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata;

import at.ebutilities.schemata.customerprocesses.masterdata._01p32.*;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.Address;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.BillingData;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.ContractPartner;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.DeliveryAddress;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.InvoiceRecipient;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.MeteringPointData;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterDataImpl;
import energy.eddie.regionconnector.at.eda.dto.masterdata.*;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.NullMeteringPointData;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;
import jakarta.annotation.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

@Mapper
public interface MasterDataMapper {

    MasterDataMapper INSTANCE = Mappers.getMapper(MasterDataMapper.class);

    @Mapping(target = "conversationId", source = "processDirectory.conversationId")
    @Mapping(target = "messageId", source = "processDirectory.messageId")
    @Mapping(target = "sector", source = "marketParticipantDirectory.sector", qualifiedByName = "sector")
    @Mapping(target = "documentCreationDateTime", source = "marketParticipantDirectory.routingHeader.documentCreationDateTime", qualifiedByName = "toUtcZonedDateTime")
    @Mapping(target = "senderMessageAddress", source = "marketParticipantDirectory.routingHeader.sender.messageAddress")
    @Mapping(target = "receiverMessageAddress", source = "marketParticipantDirectory.routingHeader.receiver.messageAddress")
    @Mapping(target = "meteringPoint", source = "processDirectory.meteringPoint")
    @Mapping(target = "meteringPointData", source = "processDirectory.meteringPointData", qualifiedByName = "meteringPointData01p32")
    @Mapping(target = "billingData", source = "processDirectory.billingData", qualifiedByName = "billingData01p32")
    @Mapping(target = "contractPartner", source = "processDirectory.contractPartner", qualifiedByName = "contractPartner01p32")
    @Mapping(target = "installationAddress", source = "processDirectory.deliveryAddress", qualifiedByName = "installationAddress01p32")
    @Mapping(target = "invoiceRecipient", source = "processDirectory.invoiceRecipient", qualifiedByName = "invoiceRecipient01p32")
    @Mapping(target = "originalMasterData", source = "masterData", qualifiedByName = "originalMasterData")
    EdaMasterDataImpl toEdaMasterData(MasterData masterData);

    @Mapping(target = "conversationId", source = "processDirectory.conversationId")
    @Mapping(target = "messageId", source = "processDirectory.messageId")
    @Mapping(target = "sector", source = "marketParticipantDirectory.sector", qualifiedByName = "sector")
    @Mapping(target = "documentCreationDateTime", source = "marketParticipantDirectory.routingHeader.documentCreationDateTime", qualifiedByName = "toUtcZonedDateTime")
    @Mapping(target = "senderMessageAddress", source = "marketParticipantDirectory.routingHeader.sender.messageAddress")
    @Mapping(target = "receiverMessageAddress", source = "marketParticipantDirectory.routingHeader.receiver.messageAddress")
    @Mapping(target = "meteringPoint", source = "processDirectory.meteringPoint")
    @Mapping(target = "meteringPointData", source = "processDirectory.meteringPointData", qualifiedByName = "meteringPointData01p33")
    @Mapping(target = "billingData", source = "processDirectory.billingData", qualifiedByName = "billingData01p33")
    @Mapping(target = "contractPartner", source = "processDirectory.contractPartner", qualifiedByName = "contractPartner01p33")
    @Mapping(target = "installationAddress", source = "processDirectory.deliveryAddress", qualifiedByName = "installationAddress01p33")
    @Mapping(target = "invoiceRecipient", source = "processDirectory.invoiceRecipient", qualifiedByName = "invoiceRecipient01p33")
    @Mapping(target = "originalMasterData", source = "masterData", qualifiedByName = "originalMasterData")
    EdaMasterDataImpl toEdaMasterData(at.ebutilities.schemata.customerprocesses.masterdata._01p33.MasterData masterData);

    @Mapping(target = "zipCode", source = "ZIP.value")
    @Mapping(target = "city", source = "city.value")
    @Mapping(target = "street", source = "street.value")
    @Mapping(target = "streetNumber", source = "streetNo.value")
    @Mapping(target = "staircase", source = "staircase.value")
    @Mapping(target = "floor", source = "floor.value")
    @Mapping(target = "door", source = "doorNumber.value")
    AddressImpl toAddress(Address address);

    @Mapping(target = "zipCode", source = "ZIP.value")
    @Mapping(target = "city", source = "city.value")
    @Mapping(target = "street", source = "street.value")
    @Mapping(target = "streetNumber", source = "streetNo.value")
    @Mapping(target = "staircase", source = "staircase.value")
    @Mapping(target = "floor", source = "floor.value")
    @Mapping(target = "door", source = "doorNumber.value")
    AddressImpl toAddress(at.ebutilities.schemata.customerprocesses.masterdata._01p33.Address address);

    @Mapping(target = "zipCode", source = "ZIP.value")
    @Mapping(target = "city", source = "city.value")
    @Mapping(target = "street", source = "street.value")
    @Mapping(target = "streetNumber", source = "streetNo.value")
    @Mapping(target = "staircase", source = "staircase.value")
    @Mapping(target = "floor", source = "floor.value")
    @Mapping(target = "door", source = "doorNumber.value")
    @Mapping(target = "addressAddition", source = "deliveryAddressData.value")
    DeliveryAddressImpl toDeliveryAddress(DeliveryAddress deliveryAddress);

    @Mapping(target = "zipCode", source = "ZIP.value")
    @Mapping(target = "city", source = "city.value")
    @Mapping(target = "street", source = "street.value")
    @Mapping(target = "streetNumber", source = "streetNo.value")
    @Mapping(target = "staircase", source = "staircase.value")
    @Mapping(target = "floor", source = "floor.value")
    @Mapping(target = "door", source = "doorNumber.value")
    @Mapping(target = "addressAddition", source = "deliveryAddressData.value")
    DeliveryAddressImpl toDeliveryAddress(at.ebutilities.schemata.customerprocesses.masterdata._01p33.DeliveryAddress deliveryAddress);

    @Mapping(target = "referenceNumber", source = "referenceNumber")
    @Mapping(target = "gridInvoiceRecipient", source = "gridInvoiceRecipient", qualifiedByName = "gridInvoiceRecipient01p32")
    @Mapping(target = "budgetBillingCycle", source = "budgetBillingCycle.value")
    @Mapping(target = "meterReadingMonth", source = "meterReadingMonth.value", defaultValue = "0")
    @Mapping(target = "consumptionBillingCycle", source = "consumptionBillingCycle.value")
    @Mapping(target = "consumptionBillingMonth", source = "consumptionBillingMonth.value", defaultValue = "0")
    @Mapping(target = "yearMonthOfNextBill", source = "yearMonthOfNextBill")
    BillingDataImpl toBillingData(BillingData billingData);

    @Mapping(target = "referenceNumber", source = "referenceNumber")
    @Mapping(target = "gridInvoiceRecipient", source = "gridInvoiceRecipient", qualifiedByName = "gridInvoiceRecipient01p33")
    @Mapping(target = "budgetBillingCycle", source = "budgetBillingCycle.value")
    @Mapping(target = "meterReadingMonth", source = "meterReadingMonth.value", defaultValue = "0")
    @Mapping(target = "consumptionBillingCycle", source = "consumptionBillingCycle.value")
    @Mapping(target = "consumptionBillingMonth", source = "consumptionBillingMonth.value", defaultValue = "0")
    @Mapping(target = "yearMonthOfNextBill", source = "yearMonthOfNextBill")
    BillingDataImpl toBillingData(at.ebutilities.schemata.customerprocesses.masterdata._01p33.BillingData billingData);

    @Mapping(target = "contractPartner", source = "partnerData")
    @Mapping(target = "address", source = "addressData")
    InvoiceRecipientImpl toInvoiceRecipient(InvoiceRecipient invoiceRecipient);

    @Mapping(target = "contractPartner", source = "partnerData")
    @Mapping(target = "address", source = "addressData")
    InvoiceRecipientImpl toInvoiceRecipient(at.ebutilities.schemata.customerprocesses.masterdata._01p33.InvoiceRecipient invoiceRecipient);

    @Named("billingData01p32")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.BillingData> billingData01p32(@Nullable BillingData billingData) {
        return Optional.ofNullable(billingData).map(this::toBillingData);
    }

    @Named("billingData01p33")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.BillingData> billingData01p33(@Nullable at.ebutilities.schemata.customerprocesses.masterdata._01p33.BillingData billingData) {
        return Optional.ofNullable(billingData).map(this::toBillingData);
    }

    @Named("installationAddress01p32")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.DeliveryAddress> installationAddress01p32(@Nullable DeliveryAddress deliveryAddress) {
        return Optional.ofNullable(deliveryAddress).map(this::toDeliveryAddress);
    }

    @Named("installationAddress01p33")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.DeliveryAddress> installationAddress01p33(@Nullable at.ebutilities.schemata.customerprocesses.masterdata._01p33.DeliveryAddress deliveryAddress) {
        return Optional.ofNullable(deliveryAddress).map(this::toDeliveryAddress);
    }

    @Named("invoiceRecipient01p32")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.InvoiceRecipient> invoiceRecipient01p32(@Nullable InvoiceRecipient invoiceRecipient) {
        return Optional.ofNullable(invoiceRecipient).map(this::toInvoiceRecipient);
    }

    @Named("invoiceRecipient01p33")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.InvoiceRecipient> invoiceRecipient01p33(@Nullable at.ebutilities.schemata.customerprocesses.masterdata._01p33.InvoiceRecipient invoiceRecipient) {
        return Optional.ofNullable(invoiceRecipient).map(this::toInvoiceRecipient);
    }

    @Named("originalMasterData")
    default Object originalMasterData(Object masterData) {
        return masterData;
    }

    @Named("sector")
    @Nullable
    default Sector sector(@Nullable String value) {
        return value == null ? null : Sector.fromValue(value);
    }

    @Named("toUtcZonedDateTime")
    @Nullable
    default ZonedDateTime toUtcZonedDateTime(@Nullable XMLGregorianCalendar calendar) {
        return calendar == null ? null : XmlGregorianCalenderUtils.toUtcZonedDateTime(calendar);
    }

    @Named("gridInvoiceRecipient01p32")
    @Nullable
    default String gridInvoiceRecipient01p32(@Nullable GridInvoiceRecipientC gridInvoiceRecipient) {
        return gridInvoiceRecipient == null ? null : gridInvoiceRecipient.getValue().value();
    }

    @Named("gridInvoiceRecipient01p33")
    @Nullable
    default String gridInvoiceRecipient01p33(@Nullable at.ebutilities.schemata.customerprocesses.masterdata._01p33.GridInvoiceRecipientC gridInvoiceRecipient) {
        return gridInvoiceRecipient == null ? null : gridInvoiceRecipient.getValue().value();
    }

    @Named("meteringPointData01p32")
    default energy.eddie.regionconnector.at.eda.dto.masterdata.MeteringPointData meteringPointData01p32(@Nullable MeteringPointData meteringPointData) {
        if (meteringPointData == null) {
            return new NullMeteringPointData();
        }
        return new MeteringPointDataImpl(
                meteringPointData.getSupStatus() == null ? null : meteringPointData.getSupStatus().value(),
                meteringPointData.getDSOTariffClass() == null ? null : meteringPointData.getDSOTariffClass()
                                                                                        .getValue()
                                                                                        .value(),
                energyDirectionOf(meteringPointData),
                meteringPointData.getEnergyCommunity() == null ? null : meteringPointData.getEnergyCommunity()
                                                                                         .getValue()
                                                                                         .value(),
                meteringPointData.getTypeOfGeneration() == null ? null : meteringPointData.getTypeOfGeneration()
                                                                                          .getValue()
                                                                                          .value(),
                meteringPointData.getLoadProfileType() == null ? null : meteringPointData.getLoadProfileType()
                                                                                         .getValue(),
                granularity01p32(meteringPointData.getDeviceType())
        );
    }

    @Named("meteringPointData01p33")
    default energy.eddie.regionconnector.at.eda.dto.masterdata.MeteringPointData meteringPointData01p33(@Nullable at.ebutilities.schemata.customerprocesses.masterdata._01p33.MeteringPointData meteringPointData) {
        if (meteringPointData == null) {
            return new NullMeteringPointData();
        }
        return new MeteringPointDataImpl(
                meteringPointData.getSupStatus() == null ? null : meteringPointData.getSupStatus().value(),
                meteringPointData.getDSOTariffClass() == null ? null : meteringPointData.getDSOTariffClass()
                                                                                        .getValue()
                                                                                        .value(),
                energyDirectionOf(meteringPointData),
                meteringPointData.getEnergyCommunity() == null ? null : meteringPointData.getEnergyCommunity()
                                                                                         .getValue()
                                                                                         .value(),
                meteringPointData.getTypeOfGeneration() == null ? null : meteringPointData.getTypeOfGeneration()
                                                                                          .getValue()
                                                                                          .value(),
                meteringPointData.getLoadProfileType() == null ? null : meteringPointData.getLoadProfileType()
                                                                                         .getValue(),
                granularity01p33(meteringPointData.getDeviceType())
        );
    }

    @Nullable
    default energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection energyDirectionOf(MeteringPointData meteringPointData) {
        var direction = meteringPointData.getEnergyDirection();
        if (direction == null) {
            return null;
        }
        return switch (direction) {
            case CONSUMPTION -> energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection.CONSUMPTION;
            case GENERATION -> energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection.GENERATION;
        };
    }

    @Nullable
    default energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection energyDirectionOf(at.ebutilities.schemata.customerprocesses.masterdata._01p33.MeteringPointData meteringPointData) {
        var direction = meteringPointData.getEnergyDirection();
        if (direction == null) {
            return null;
        }
        return switch (direction) {
            case CONSUMPTION -> energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection.CONSUMPTION;
            case GENERATION -> energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection.GENERATION;
        };
    }

    default Granularity granularity01p32(@Nullable DeviceTypeC deviceType) {
        if (deviceType == null) {
            return Granularity.P1D;
        }
        return deviceType.getValue() == DeviceType.IME ? Granularity.PT15M : Granularity.P1D;
    }

    default Granularity granularity01p33(@Nullable at.ebutilities.schemata.customerprocesses.masterdata._01p33.DeviceTypeC deviceType) {
        if (deviceType == null) {
            return Granularity.P1D;
        }
        return deviceType.getValue() == at.ebutilities.schemata.customerprocesses.masterdata._01p33.DeviceType.IME
                ? Granularity.PT15M
                : Granularity.P1D;
    }

    @Named("contractPartner01p32")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.ContractPartner> contractPartner01p32(@Nullable ContractPartner contractPartner) {
        return Optional.ofNullable(contractPartner).map(this::toContractPartner);
    }

    @Named("contractPartner01p33")
    default Optional<energy.eddie.regionconnector.at.eda.dto.masterdata.ContractPartner> contractPartner01p33(@Nullable at.ebutilities.schemata.customerprocesses.masterdata._01p33.ContractPartner contractPartner) {
        return Optional.ofNullable(contractPartner).map(this::toContractPartner);
    }

    default ContractPartnerImpl toContractPartner(ContractPartner contractPartner) {
        return new ContractPartnerImpl(
                contractPartner.getSalutation(),
                contractPartner.getName1() == null ? null : contractPartner.getName1().getValue(),
                contractPartner.getName2() == null ? null : contractPartner.getName2().getValue(),
                join(
                        contractPartner.getName1() == null ? null : contractPartner.getName1().getValue(),
                        contractPartner.getName2() == null ? null : contractPartner.getName2().getValue(),
                        contractPartner.getName3() == null ? null : contractPartner.getName3().getValue(),
                        contractPartner.getName4() == null ? null : contractPartner.getName4().getValue()
                ),
                contractPartner.getContractPartnerNumber(),
                toZonedDateTime(contractPartner.getDateOfBirth()),
                toZonedDateTime(contractPartner.getDateOfDeath()),
                contractPartner.getCompanyRegistryNo(),
                contractPartner.getVATNumber(),
                contractPartner.getEmail()
        );
    }

    default ContractPartnerImpl toContractPartner(at.ebutilities.schemata.customerprocesses.masterdata._01p33.ContractPartner contractPartner) {
        return new ContractPartnerImpl(
                contractPartner.getSalutation(),
                contractPartner.getName1() == null ? null : contractPartner.getName1().getValue(),
                contractPartner.getName2() == null ? null : contractPartner.getName2().getValue(),
                join(
                        contractPartner.getName1() == null ? null : contractPartner.getName1().getValue(),
                        contractPartner.getName2() == null ? null : contractPartner.getName2().getValue()
                ),
                contractPartner.getContractPartnerNumber(),
                toZonedDateTime(contractPartner.getDateOfBirth()),
                toZonedDateTime(contractPartner.getDateOfDeath()),
                contractPartner.getCompanyRegistryNo(),
                contractPartner.getVATNumber(),
                contractPartner.getEmail()
        );
    }

    @Nullable
    default String join(@Nullable String... names) {
        if (names == null) {
            return null;
        }
        return Stream.of(names)
                     .filter(name -> name != null && !name.isBlank())
                     .reduce((a, b) -> a + " " + b)
                     .orElse(null);
    }

    @Nullable
    default ZonedDateTime toZonedDateTime(@Nullable XMLGregorianCalendar calendar) {
        // ponytail: XmlGregorianCalenderUtils.toUtcZonedDateTime, plain toGregorianCalendar() recurses
        // in ZoneInfo when the timezone is undefined (JDK bug), which the original lazy wrappers
        // only triggered on access; eager mapping hits it at construction.
        return calendar == null ? null : XmlGregorianCalenderUtils.toUtcZonedDateTime(calendar);
    }
}
