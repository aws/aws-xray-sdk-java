package com.amazonaws.xray.utils;

import static com.amazonaws.xray.utils.LooseValidations.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.xray.utils.LooseValidations.ValidationMode;
import org.junit.Test;

public class LooseValidationsTest {

    @Test
    public void checkNotNull_notNull() {
        assertThat(checkNotNull("bar", "foo")).isTrue();
    }

    @Test
    public void checkNotNull_null() {
        assertThat(checkNotNull(null, "foo")).isFalse();
    }

    @Test
    public void validationModeParsing() {
        assertThat(LooseValidations.validationMode("none")).isEqualTo(ValidationMode.NONE);
        assertThat(LooseValidations.validationMode("NONE")).isEqualTo(ValidationMode.NONE);
        assertThat(LooseValidations.validationMode("log")).isEqualTo(ValidationMode.LOG);
        // Check mixed case
        assertThat(LooseValidations.validationMode("Log")).isEqualTo(ValidationMode.LOG);
        assertThat(LooseValidations.validationMode("throw")).isEqualTo(ValidationMode.THROW);
        assertThat(LooseValidations.validationMode("")).isEqualTo(ValidationMode.NONE);
        assertThat(LooseValidations.validationMode("unknown")).isEqualTo(ValidationMode.NONE);
    }
}
