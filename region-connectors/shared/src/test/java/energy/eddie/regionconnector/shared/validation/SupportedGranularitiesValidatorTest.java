package energy.eddie.regionconnector.shared.validation;

import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestConfig.class)
class SupportedGranularitiesValidatorTest {
    @Autowired
    private Validator validator;

    @Test
    void whenValidValue_thenNoConstraintViolations() {
        MyTest obj = new MyTest();
        obj.granularity = Granularity.P1D;

        assertTrue(validator.validate(obj).isEmpty());
    }

    @Test
    void whenNull_thenNoConstraintViolations() {
        MyTest obj = new MyTest();
        obj.granularity = null;

        assertTrue(validator.validate(obj).isEmpty());
    }

    @Test
    void whenInvalidValue_thenConstraintViolations() {
        MyTest obj = new MyTest();
        obj.granularity = Granularity.P1Y;

        var violations = validator.validate(obj);
        assertEquals(1, violations.size());

        assertFalse(validator.validate(obj).isEmpty());
    }

    static class MyTest {
        @SupportedGranularities({Granularity.P1D, Granularity.PT1H})
        @Nullable
        @SuppressWarnings("unused")
        private Granularity granularity;
    }
}