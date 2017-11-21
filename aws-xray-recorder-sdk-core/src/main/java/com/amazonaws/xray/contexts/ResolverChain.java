package com.amazonaws.xray.contexts;

public interface ResolverChain<T> {
    public T resolve();
}
