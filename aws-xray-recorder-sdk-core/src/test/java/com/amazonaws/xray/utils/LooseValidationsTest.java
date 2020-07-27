/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.xray.utils;

import static com.amazonaws.xray.utils.LooseValidations.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.xray.utils.LooseValidations.ValidationMode;
import org.junit.jupiter.api.Test;

class LooseValidationsTest {

    @Test
    void checkNotNull_notNull() {
        assertThat(checkNotNull("bar", "foo")).isTrue();
    }

    @Test
    void checkNotNull_null() {
        assertThat(checkNotNull(null, "foo")).isFalse();
    }

    @Test
    void validationModeParsing() {
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
