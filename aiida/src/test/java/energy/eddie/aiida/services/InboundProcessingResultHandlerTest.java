// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

class InboundProcessingResultHandlerTest {
    @Test
    void handle_returnsRecordAndResultToSubscriber() {
        var resultHandler = new InboundProcessingResultHandler();
        var inboundRecord = mock(InboundRecord.class);
        var processingResult = InboundProcessingResult.rejected("invalid permission id");

        StepVerifier.create(resultHandler.flux())
                    .then(() -> resultHandler.handle(inboundRecord, processingResult))
                    .expectNext(new InboundProcessingResultHandler.ProcessedInboundRecord(inboundRecord,
                                                                                          processingResult))
                    .thenCancel()
                    .verify();
    }
}
