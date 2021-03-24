package com.amazonaws.xray.entities;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.amazonaws.xray.AWSXRay;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EntityTest {

    @Test
    void testTimeModuleSerialization() {
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(),"test");
        Duration duration = Duration.ofSeconds(1);
        seg.putMetadata("meta", duration);
        String serializedSeg = seg.serialize();

        String expected = "{\"default\":{\"meta\":1.000000000}}}";
        assertThat(serializedSeg).contains(expected);
    }
}
