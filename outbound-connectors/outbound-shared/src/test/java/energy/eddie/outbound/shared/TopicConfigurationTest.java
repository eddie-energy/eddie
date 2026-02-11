// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicConfigurationTest {
    private final TopicConfiguration config = new TopicConfiguration("e123");

    @Test
    void testRawDataMessage_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.rawDataMessage();

        // Then
        assertEquals("ep.e123.agnostic.raw-data-message", res);
    }

    @Test
    void testConnectionStatusMessage_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.connectionStatusMessage();

        // Then
        assertEquals("ep.e123.agnostic.connection-status-message", res);
    }

    @Test
    void testPermissionMarketDocument_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.permissionMarketDocument();

        // Then
        assertEquals("ep.e123.cim_0_82.permission-md", res);
    }

    @Test
    void testValidatedHistoricalDataMarketDocument_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_0_82);

        // Then
        assertEquals("ep.e123.cim_0_82.validated-historical-data-md", res);
    }

    @Test
    void testNearRealTimeDataMarketDocument_V1_04_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_04);

        // Then
        assertEquals("ep.e123.cim_1_04.near-real-time-data-md", res);
    }

    @Test
    void testNearRealTimeDataMarketDocument_V1_12_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_12);

        // Then
        assertEquals("ep.e123.cim_1_12.near-real-time-data-md", res);
    }

    @Test
    void testAccountingPointDataMarketDocument_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.accountingPointMarketDocument();

        // Then
        assertEquals("ep.e123.cim_0_82.accounting-point-md", res);
    }

    @Test
    void testTerminationMarketDocument_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.terminationMarketDocument();

        // Then
        assertEquals("fw.e123.cim_0_82.termination-md", res);
    }

    @Test
    void testRedistributionTransactionRequestDocument_returnsCorrectTopicName() {
        // Given
        // When
        var res = config.redistributionTransactionRequestDocument();

        // Then
        assertEquals("fw.e123.cim_0_91_08.redistribution-transaction-rd", res);
    }
}