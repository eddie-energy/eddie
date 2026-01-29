// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.cim;

import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.providers.ap.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CimStruct {
    private final List<AccountsEndpoint200ResponseAllOfAccountsInner> rawAccounts;
    private final List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner> rawServiceContracts;
    private final List<ServicePointEndpoint200ResponseAllOfServicePointsInner> rawServicePoints;
    private final List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner> rawMeterDevices;
    private final List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> rawUsageSegments;

    public CimStruct(IdentifiableValidatedHistoricalData historicalData) {
        rawAccounts = historicalData.payload().accounts();
        rawServiceContracts = historicalData.payload().serviceContracts();
        rawServicePoints = historicalData.payload().servicePoints();
        rawMeterDevices = historicalData.payload().meterDevices();
        rawUsageSegments = historicalData.payload().usageSegments();
    }

    public CimStruct(IdentifiableAccountingPointData accountingPointData) {
        rawAccounts = accountingPointData.payload().accounts();
        rawServiceContracts = accountingPointData.payload().serviceContracts();
        rawServicePoints = accountingPointData.payload().servicePoints();
        rawMeterDevices = accountingPointData.payload().meterDevices();
        rawUsageSegments = List.of();
    }

    public List<Account> get() {
        var accounts = new ArrayList<Account>();
        for (var account : rawAccounts) {
            var contracts = getServiceContracts(account);
            var acc = new Account(account.getCustomerNumber(),
                                  account.getAccountName(),
                                  account.getAccountType(),
                                  contracts);
            accounts.add(acc);
        }
        return accounts;
    }


    private List<ServiceContract> getServiceContracts(AccountsEndpoint200ResponseAllOfAccountsInner account) {
        var contracts = findServiceContractByAccountId(account.getCdsAccountId());
        var serviceContracts = new ArrayList<ServiceContract>();
        for (var contract : contracts) {
            var servicePoints = getServicePoints(contract);
            serviceContracts.add(new ServiceContract(contract.getContractAddress(),
                                                     contract.getServiceType(),
                                                     servicePoints));
        }
        return serviceContracts;
    }

    private List<ServicePoint> getServicePoints(ServiceContractEndpoint200ResponseAllOfServiceContractsInner contract) {
        var points = findServicePointsByServiceContract(contract.getCdsServicecontractId());
        var servicePoints = new ArrayList<ServicePoint>();
        for (var point : points) {
            var meterDevices = getMeters(point);
            servicePoints.add(new ServicePoint(point.getServicepointAddress(), meterDevices));
        }
        return servicePoints;
    }


    private List<Meter> getMeters(ServicePointEndpoint200ResponseAllOfServicePointsInner servicePoint) {
        var meters = new ArrayList<Meter>();
        var meterDevices = findMeterDevicesByServicePoint(servicePoint.getCdsServicepointId());
        for (var meterDevice : meterDevices) {
            var segments = findUsageSegmentsByMeter(meterDevice.getCdsMeterdeviceId());
            var usageSegments = getUsageSegments(segments);
            var meter = new Meter(meterDevice.getMeterNumber(), meterDevice.getCdsMeterdeviceId(), usageSegments);
            meters.add(meter);
        }
        return meters;
    }

    private static List<UsageSegment> getUsageSegments(List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> segments) {
        var usageSegments = new ArrayList<UsageSegment>();
        for (var segment : segments) {
            var usageSegmentValues = flattenUsageSegmentValues(segment);
            var usageSegment = new UsageSegment(segment.getSegmentStart().toZonedDateTime(),
                                                segment.getSegmentEnd().toZonedDateTime(),
                                                segment.getInterval(),
                                                usageSegmentValues);
            usageSegments.add(usageSegment);
        }
        return usageSegments;
    }

    private static Map<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum, List<BigDecimal>> flattenUsageSegmentValues(
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner segment
    ) {
        Map<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum, List<BigDecimal>> usageSegmentValues = new EnumMap<>(
                UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.class);
        for (var values : segment.getValues()) {
            for (var i = 0; i < segment.getFormat().size(); i++) {
                var format = segment.getFormat().get(i);
                var value = values.get(i).getOrDefault("v", null);
                var oldValues = usageSegmentValues.getOrDefault(format, new ArrayList<>());
                oldValues.addLast(value);
                usageSegmentValues.put(format, oldValues);
            }
        }
        return usageSegmentValues;
    }

    private List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner> findServiceContractByAccountId(String accountId) {
        return rawServiceContracts.stream()
                                  .filter(contract -> contract.getCdsAccountId().equals(accountId))
                                  .toList();
    }

    private List<ServicePointEndpoint200ResponseAllOfServicePointsInner> findServicePointsByServiceContract(String contractNumber) {
        return rawServicePoints.stream()
                               .filter(point -> point.getCurrentServicecontracts().contains(contractNumber))
                               .toList();
    }

    private List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner> findMeterDevicesByServicePoint(String cdsServicepointId) {
        return rawMeterDevices.stream()
                              .filter(meter -> meter.getCurrentServicepoints().contains(cdsServicepointId))
                              .toList();
    }

    private List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> findUsageSegmentsByMeter(String meterNumber) {
        return rawUsageSegments.stream()
                               .filter(usageSegment -> usageSegment.getRelatedMeterdevices().contains(meterNumber))
                               .toList();
    }
}
