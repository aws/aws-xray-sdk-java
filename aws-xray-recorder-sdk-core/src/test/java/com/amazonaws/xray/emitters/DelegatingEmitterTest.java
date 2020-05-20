package com.amazonaws.xray.emitters;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.Subsegment;
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

    @Test
    public void delegates() {
        Emitter delegator = new DelegatingEmitter(emitter);

        assertThat(delegator.sendSegment(segment));

    }
}
