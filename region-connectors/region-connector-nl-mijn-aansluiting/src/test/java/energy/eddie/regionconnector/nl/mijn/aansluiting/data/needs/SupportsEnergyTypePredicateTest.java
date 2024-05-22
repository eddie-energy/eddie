package energy.eddie.regionconnector.nl.mijn.aansluiting.data.needs;

import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportsEnergyTypePredicateTest {
    @Mock
    private AiidaDataNeed aiidaDataNeed;
    @Mock
    private ValidatedHistoricalDataDataNeed dataNeed;

    @Test
    void testSupportsEnergyTypePredicate_forIrrelevantDataNeed_returnsTrue() {
        // Given
        var predicate = new SupportsEnergyTypePredicate();

        // When
        var res = predicate.test(aiidaDataNeed);

        // Then
        assertTrue(res);
    }

    @Test
    void testSupportsEnergyTypePredicate_forUnsupportedEnergyType_returnsFalse() {
        // Given
        var predicate = new SupportsEnergyTypePredicate();
        when(dataNeed.energyType())
                .thenReturn(EnergyType.HYDROGEN);

        // When
        var res = predicate.test(dataNeed);

        // Then
        assertFalse(res);
    }

    @Test
    void testSupportsEnergyTypePredicate_forSupportedEnergyType_returnsFalse() {
        // Given
        var predicate = new SupportsEnergyTypePredicate();
        when(dataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);

        // When
        var res = predicate.test(dataNeed);

        // Then
        assertTrue(res);
    }
}