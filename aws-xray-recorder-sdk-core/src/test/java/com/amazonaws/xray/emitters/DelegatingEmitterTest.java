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

package com.amazonaws.xray.emitters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DelegatingEmitterTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private Emitter emitter;

    @Mock
    private Segment segment;

    @Mock
    private Subsegment subsegment;

    @Before
    public void setUp() {
        when(emitter.sendSegment(any())).thenReturn(true);
        when(emitter.sendSubsegment(any())).thenReturn(true);
    }

    @Test
    public void delegates() {
        Emitter delegator = new DelegatingEmitter(emitter);

        assertThat(delegator.sendSegment(segment)).isTrue();
        verify(emitter).sendSegment(segment);

        assertThat(delegator.sendSubsegment(subsegment)).isTrue();
        verify(emitter).sendSubsegment(subsegment);

    }
}
