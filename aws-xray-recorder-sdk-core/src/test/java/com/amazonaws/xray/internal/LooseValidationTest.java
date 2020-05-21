package com.amazonaws.xray.internal;

import static com.amazonaws.xray.internal.LooseValidation.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class LooseValidationTest {

    @Test
    public void checkNotNull_notNull() {
        assertThat(checkNotNull("bar", "foo")).isTrue();
    }

    @Test
    public void checkNotNull_null() {
        assertThat(checkNotNull(null, "foo")).isFalse();
    }
}
