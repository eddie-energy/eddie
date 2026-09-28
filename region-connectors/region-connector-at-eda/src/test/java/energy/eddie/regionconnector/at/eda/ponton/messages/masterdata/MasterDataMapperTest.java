// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.ebutilities.schemata.customerprocesses.masterdata._01p32.*;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.NullMeteringPointData;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests pinning the MapStruct mapping to the values the hand-written
 * {@code EdaMasterData01p32}/{@code EdaMasterData01p33} wrapper records produced.
 */
class MasterDataMapperTest {

    @Test
    @SuppressWarnings("java:S5961")
    void test01p32FullMapping() {
        // Given
        var masterData = fullMasterData();

        // When
        var res = MasterDataMapper.INSTANCE.toEdaMasterData(masterData);

        // Then
        assertEquals("conv-1", res.conversationId());
        assertEquals("msg-1", res.messageId());
        assertEquals(Sector.ELECTRICITY, res.sector());
        assertEquals(ZonedDateTime.of(2026, 3, 10, 12, 30, 45, 0, ZoneOffset.UTC), res.documentCreationDateTime());
        assertEquals("sender-ec", res.senderMessageAddress());
        assertEquals("receiver-dso", res.receiverMessageAddress());
        assertEquals("AT1111111111111111111111111111111", res.meteringPoint());
        assertSame(masterData, res.originalMasterData());

        var mpd = res.meteringPointData();
        assertEquals("ON", mpd.supStatus());
        assertEquals("G", mpd.dsoTariff());
        assertEquals(energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection.CONSUMPTION, mpd.energyDirection());
        assertEquals("GC", mpd.energyCommunity());
        assertEquals("FULL", mpd.typeOfGeneration());
        assertEquals("L1", mpd.loadProfileType());
        assertEquals(Granularity.PT15M, mpd.granularity());

        var billingData = res.billingData().orElseThrow();
        assertEquals("ref-123", billingData.referenceNumber());
        assertEquals("CUSTOMER", billingData.gridInvoiceRecipient());
        assertEquals("monthly", billingData.budgetBillingCycle());
        assertEquals(0, billingData.meterReadingMonth());
        assertEquals("yearly", billingData.consumptionBillingCycle());
        assertEquals(0, billingData.consumptionBillingMonth());
        assertEquals("2026-05", billingData.yearMonthOfNextBill());

        var contractPartner = res.contractPartner().orElseThrow();
        assertEquals("Dr", contractPartner.salutation());
        assertEquals("Ada", contractPartner.surname());
        assertEquals("Lovelace", contractPartner.firstName());
        assertEquals("Ada Lovelace Countess Lovelace", contractPartner.companyName());
        assertEquals("cp-1", contractPartner.contractPartnerNumber());
        // UTC conversion is a deliberate deviation from the old lazy wrapper, which used the default timezone
        assertEquals(ZonedDateTime.of(1990, 5, 17, 0, 0, 0, 0, ZoneOffset.UTC), contractPartner.dateOfBirth());
        assertNull(contractPartner.dateOfDeath());
        assertEquals("CRN-1", contractPartner.companyRegisterNumber());
        assertEquals("ATU123", contractPartner.vatNumber());
        assertEquals("ada@example.com", contractPartner.email());

        var installationAddress = res.installationAddress().orElseThrow();
        assertEquals("4020", installationAddress.zipCode());
        assertEquals("Linz", installationAddress.city());
        assertEquals("Hauptstr", installationAddress.street());
        assertEquals("12a", installationAddress.streetNumber());
        assertEquals("2", installationAddress.staircase());
        assertEquals("3", installationAddress.floor());
        assertEquals("7", installationAddress.door());
        assertEquals("top 5", installationAddress.addressAddition());

        var invoiceRecipient = res.invoiceRecipient().orElseThrow();
        assertEquals("Grace", invoiceRecipient.contractPartner().surname());
        assertEquals("CRN-2", invoiceRecipient.contractPartner().companyRegisterNumber());
        assertEquals("1010", invoiceRecipient.address().zipCode());
        assertEquals("Wien", invoiceRecipient.address().city());
    }

    @Test
    void test01p32EmptyMasterData_returnsNullObjectAndEmptyOptionals() {
        // Given
        var masterData = new MasterData().withProcessDirectory(new ProcessDirectory());

        // When
        var res = MasterDataMapper.INSTANCE.toEdaMasterData(masterData);

        // Then
        assertInstanceOf(NullMeteringPointData.class, res.meteringPointData());
        assertEquals(Optional.empty(), res.billingData());
        assertEquals(Optional.empty(), res.contractPartner());
        assertEquals(Optional.empty(), res.installationAddress());
        assertEquals(Optional.empty(), res.invoiceRecipient());
        assertNull(res.sector());
    }

    @Test
    void test01p33CompanyName_joinsOnlyName1AndName2() {
        // Given
        at.ebutilities.schemata.customerprocesses.masterdata._01p33.ContractPartner jaxbContractPartner =
                new at.ebutilities.schemata.customerprocesses.masterdata._01p33.ContractPartner()
                        .withName1(new at.ebutilities.schemata.customerprocesses.masterdata._01p33.NameC().withValue(
                                "Ada"))
                        .withName2(new at.ebutilities.schemata.customerprocesses.masterdata._01p33.NameC().withValue(
                                "Lovelace"));
        var masterData = new at.ebutilities.schemata.customerprocesses.masterdata._01p33.MasterData()
                .withProcessDirectory(
                        new at.ebutilities.schemata.customerprocesses.masterdata._01p33.ProcessDirectory()
                                .withContractPartner(jaxbContractPartner)
                );

        // When
        var res = MasterDataMapper.INSTANCE.toEdaMasterData(masterData);

        // Then
        var contractPartner = res.contractPartner().orElseThrow();
        assertEquals("Ada Lovelace", contractPartner.companyName());
        // deliberate bugfix: the old 01p33 wrapper threw an NPE on missing dates
        assertNull(contractPartner.dateOfBirth());
        assertNull(contractPartner.dateOfDeath());
    }

    @Test
    void testMeteringPointDataDefaults_deviceTypeNullMapsToP1D_andGenerationDirection() {
        // Given
        var masterData = new MasterData()
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withMeteringPointData(
                                        new MeteringPointData()
                                                .withEnergyDirection(EnergyDirection.GENERATION)
                                )
                );

        // When
        var res = MasterDataMapper.INSTANCE.toEdaMasterData(masterData);

        // Then
        var mpd = res.meteringPointData();
        assertEquals(Granularity.P1D, mpd.granularity());
        assertEquals(energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection.GENERATION, mpd.energyDirection());
    }

    private static MasterData fullMasterData() {
        var factory = DatatypeFactory.newDefaultInstance();
        var dateOfBirth = factory.newXMLGregorianCalendar(1990, 5, 17, 0, 0, 0, 0, 0);
        var creationDateTime = factory.newXMLGregorianCalendar(2026, 3, 10, 12, 30, 45, 0, 0);
        return new MasterData()
                .withMarketParticipantDirectory(
                        new MarketParticipantDirectory()
                                .withSector("01")
                                .withRoutingHeader(
                                        new RoutingHeader()
                                                .withSender(new RoutingAddress().withMessageAddress("sender-ec"))
                                                .withReceiver(new RoutingAddress().withMessageAddress("receiver-dso"))
                                                .withDocumentCreationDateTime(creationDateTime)
                                )
                )
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withConversationId("conv-1")
                                .withMessageId("msg-1")
                                .withMeteringPoint("AT1111111111111111111111111111111")
                                .withMeteringPointData(
                                        new MeteringPointData()
                                                .withSupStatus(SupStatus.ON)
                                                .withDSOTariffClass(new DSOTariffClassC().withValue(DSOTariffClass.G))
                                                .withEnergyDirection(EnergyDirection.CONSUMPTION)
                                                .withEnergyCommunity(new EnergyCommunityC().withValue(EnergyCommunity.GC))
                                                .withTypeOfGeneration(new TypeOfGenerationC().withValue(TypeOfGeneration.FULL))
                                                .withLoadProfileType(new LoadProfileTypeC().withValue("L1"))
                                                .withDeviceType(new DeviceTypeC().withValue(DeviceType.IME))
                                )
                                .withBillingData(
                                        new BillingData()
                                                .withReferenceNumber("ref-123")
                                                .withGridInvoiceRecipient(new GridInvoiceRecipientC().withValue(
                                                        GridInvoiceRecipient.CUSTOMER))
                                                .withBudgetBillingCycle(new BudgetBillingCycleC().withValue("monthly"))
                                                .withConsumptionBillingCycle(new ConsumptionBillingCycleC().withValue(
                                                        "yearly"))
                                                .withYearMonthOfNextBill("2026-05")
                                )
                                .withContractPartner(
                                        new ContractPartner()
                                                .withSalutation("Dr")
                                                .withName1(new NameC().withValue("Ada"))
                                                .withName2(new NameC().withValue("Lovelace"))
                                                .withName3(new NameC().withValue("Countess"))
                                                .withName4(new NameC().withValue("Lovelace"))
                                                .withContractPartnerNumber("cp-1")
                                                .withDateOfBirth(dateOfBirth)
                                                .withCompanyRegistryNo("CRN-1")
                                                .withVATNumber("ATU123")
                                                .withEmail("ada@example.com")
                                )
                                .withDeliveryAddress(
                                        new DeliveryAddress()
                                                .withZIP(new ZIPC().withValue("4020"))
                                                .withCity(new CityC().withValue("Linz"))
                                                .withStreet(new StreetC().withValue("Hauptstr"))
                                                .withStreetNo(new StreetNoC().withValue("12a"))
                                                .withStaircase(new StaircaseC().withValue("2"))
                                                .withFloor(new FloorC().withValue("3"))
                                                .withDoorNumber(new DoorNumberC().withValue("7"))
                                                .withDeliveryAddressData(new DeliveryAddressDataC().withValue("top 5"))
                                )
                                .withInvoiceRecipient(
                                        new InvoiceRecipient()
                                                .withPartnerData(
                                                        new ContractPartner()
                                                                .withSalutation("Ms")
                                                                .withName1(new NameC().withValue("Grace"))
                                                                .withName2(new NameC().withValue("Hopper"))
                                                                .withCompanyRegistryNo("CRN-2")
                                                )
                                                .withAddressData(
                                                        new Address()
                                                                .withZIP(new ZIPC().withValue("1010"))
                                                                .withCity(new CityC().withValue("Wien"))
                                                )
                                )
                );
    }
}
