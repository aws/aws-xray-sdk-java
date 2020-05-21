package com.amazonaws.xray.utils;

import static com.amazonaws.xray.utils.LooseValidations.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;

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
}
