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

package com.amazonaws.xray.contexts;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SegmentContextResolverChain implements ResolverChain<SegmentContext> {

    private final List<SegmentContextResolver> resolvers = new ArrayList<>();

    public void addResolver(SegmentContextResolver resolver) {
        resolvers.add(resolver);
    }

    @Override
    @Nullable
    public SegmentContext resolve() {
        for (SegmentContextResolver resolver : resolvers) {
            SegmentContext ctx = resolver.resolve();
            if (ctx != null) {
                return ctx;
            }
        }

        return null;
    }
}
