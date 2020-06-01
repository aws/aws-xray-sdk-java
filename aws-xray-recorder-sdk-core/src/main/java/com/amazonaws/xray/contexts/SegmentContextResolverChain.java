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
import java.util.Optional;

public class SegmentContextResolverChain implements ResolverChain<SegmentContext> {

    private List<SegmentContextResolver> resolvers = new ArrayList<>();

    public void addResolver(SegmentContextResolver resolver) {
        resolvers.add(resolver);
    }

    public SegmentContext resolve() {
        Optional<SegmentContextResolver> firstResolver = resolvers.stream().filter(resolver -> {
            return null != resolver.resolve();
        }).findFirst();

        if (firstResolver.isPresent()) {
            return firstResolver.get().resolve();
        }
        return null;
    }
}
