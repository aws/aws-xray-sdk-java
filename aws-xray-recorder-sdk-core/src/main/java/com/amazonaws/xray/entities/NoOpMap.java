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

package com.amazonaws.xray.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * An empty {@link Map} which does nothing, but unlike {@link Collections#emptyMap()} won't throw
 * {@link UnsupportedOperationException}.
 */
// We don't actually use the type parameters so nullness annotations would be ignored anyways so no need to annotate.
@SuppressWarnings("all")
class NoOpMap implements Map<Object, Object> {

    static <K, V> Map<K, V> get() {
        return (Map<K, V>) INSTANCE;
    }

    private static final NoOpMap INSTANCE = new NoOpMap();

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public Object put(Object key, Object value) {
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> m) {
    }

    @Override
    public void clear() {
    }

    @Override
    public Collection<Object> values() {
        return NoOpList.get();
    }

    @Override
    public Set<Object> keySet() {
        return NoOpSet.get();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return NoOpSet.get();
    }

}
