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
