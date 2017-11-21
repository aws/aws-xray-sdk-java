package com.amazonaws.xray.contexts;

public class ThreadLocalSegmentContextResolver implements SegmentContextResolver {

    @Override
    public SegmentContext resolve() {
        return new ThreadLocalSegmentContext();
    }
}
