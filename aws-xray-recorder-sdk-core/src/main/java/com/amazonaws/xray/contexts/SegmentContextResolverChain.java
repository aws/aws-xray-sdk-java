package com.amazonaws.xray.contexts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SegmentContextResolverChain implements ResolverChain<SegmentContext> {

    private List<SegmentContextResolver> resolvers = new ArrayList<>();

    public void addResolver(SegmentContextResolver resolver) {
        resolvers.add(resolver);
    }

    public SegmentContext resolve() {
        Optional<SegmentContextResolver> firstResolver = resolvers.stream().filter( (resolver) -> {
            return null != resolver.resolve();
        }).findFirst();

        if (firstResolver.isPresent()) {
            return firstResolver.get().resolve();
        }
        return null;
    }
}
