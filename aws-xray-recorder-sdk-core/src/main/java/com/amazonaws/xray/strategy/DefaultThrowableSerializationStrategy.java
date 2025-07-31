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

package com.amazonaws.xray.strategy;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.ThrowableDescription;
import com.amazonaws.xray.internal.IdGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * Default implementation of {@code ThrowableSerializationStrategy}.
 * This class auto-registers the java.io.UncheckedIOException class as a remote exception class if no set of remote exception
 * classes is provided in the constructor.
 */
public class DefaultThrowableSerializationStrategy implements ThrowableSerializationStrategy {
    private static final int DEFAULT_MAX_STACK_TRACE_LENGTH = 50;
    private static final Set<Class<? extends Throwable>> DEFAULT_REMOTE_EXCEPTION_CLASSES =
        Collections.singleton(java.io.UncheckedIOException.class);

    private final int maxStackTraceLength;
    private final Set<Class<? extends Throwable>> remoteExceptionClasses;

    public DefaultThrowableSerializationStrategy() {
        this(DEFAULT_MAX_STACK_TRACE_LENGTH);
    }

    /**
     * Constructs a new instance of {@code DefaultThrowableSerializationStrategy}, overriding the max stack trace length default
     * value of 50. Use this constructor to include more or less stack trace
     * information in (sub)segments.
     *
     * @param maxStackTraceLength
     *            the maximum number of stack trace elements to include in a single (sub)segment.
     */
    public DefaultThrowableSerializationStrategy(int maxStackTraceLength) {
        this(maxStackTraceLength, DEFAULT_REMOTE_EXCEPTION_CLASSES);
    }

    /**
     * Constructs a new instance of {@code DefaultThrowableSerializationStrategy}, overriding the max stack trace length default
     * value of 50, and overriding the Throwable classes considered 'remote'. Use this constructor to include more or less stack
     * trace information in (sub)segments.
     *
     * @param maxStackTraceLength
     *            the maximum number of stack trace elements to include in a single (sub)segment.
     * @param remoteExceptionClasses
     *            the superclasses which extend {@code Throwable} for which exceptions should be considered remote.
     */
    public DefaultThrowableSerializationStrategy(
        int maxStackTraceLength, Set<Class<? extends Throwable>> remoteExceptionClasses) {
        this.maxStackTraceLength = maxStackTraceLength;
        this.remoteExceptionClasses = remoteExceptionClasses;
    }

    private boolean isRemote(Throwable throwable) {
        return remoteExceptionClasses.parallelStream().anyMatch(remoteExceptionClass -> {
            return remoteExceptionClass.isInstance(throwable);
        });
    }

    private Optional<ThrowableDescription> referenceInChildren(Throwable throwable, List<Subsegment> subsegments) {
        return subsegments.parallelStream()
            .flatMap(subsegment -> subsegment.getCause().getExceptions().stream())
            .filter(throwableDescription -> throwable.equals(throwableDescription.getThrowable()))
            .findAny();
    }

    private ThrowableDescription describeThrowable(Throwable throwable, String id) {
        ThrowableDescription description = new ThrowableDescription();

        description.setId(id);
        description.setMessage(throwable.getMessage());
        description.setType(throwable.getClass().getName());

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null && stackTrace.length > maxStackTraceLength) {
            description.setStack(Arrays.copyOfRange(stackTrace, 0, maxStackTraceLength));
            description.setTruncated(stackTrace.length - maxStackTraceLength);
        } else {
            description.setStack(stackTrace);
        }
        description.setThrowable(throwable);

        if (isRemote(throwable)) {
            description.setRemote(true);
        }

        return description;
    }

    @Override
    public List<ThrowableDescription> describeInContext(Throwable throwable, List<Subsegment> subsegments) {
        return describeInContext(null, throwable, subsegments);
    }

    @Override
    public List<ThrowableDescription> describeInContext(
        @Nullable Entity entity,
        Throwable throwable,
        List<Subsegment> subsegments
    ) {
        List<ThrowableDescription> result = new ArrayList<>();
        IdGenerator idGenerator;

        // We could fall back on ThreadLocalStorage#get if we were confident that no `SegmentContext` implementations
        // override the default get/setTraceEntity implementations, but we can't make that guarantee.
        if (entity != null) {
            idGenerator = entity.getCreator().getIdGenerator();
        } else if (!subsegments.isEmpty()) {
            idGenerator = subsegments.get(0).getCreator().getIdGenerator();
        } else {
            idGenerator = AWSXRay.getGlobalRecorder().getIdGenerator();
        }

        /*
         * Visit each node in the cause chain. For each node:
         *  Determine if it has already been described in one of the child subsegments' causes. If so, link there.
         *  Otherwise, describe it and add it to the result.
         */

        ThrowableDescription description = new ThrowableDescription();

        Optional<ThrowableDescription> exceptionReferenced = referenceInChildren(throwable, subsegments);

        if (exceptionReferenced.isPresent()) {
            //already described, we can link to this one by ID. Get the id from the child's Throwabledescription (if it has one).
            //Use the cause otherwise.

            ThrowableDescription exception = exceptionReferenced.get();
            description.setCause(exception.getId() != null ? exception.getId() : exception.getCause());
            description.setThrowable(throwable);
            result.add(description);
            return result;
        } else {
            description = describeThrowable(throwable, idGenerator.newEntityId());
            result.add(description);
        }

        Throwable nextNode = throwable.getCause();
        while (null != nextNode) {
            final Throwable currentNode = nextNode;
            exceptionReferenced = referenceInChildren(currentNode, subsegments);

            if (exceptionReferenced.isPresent()) {
                description.setCause(null == exceptionReferenced.get().getId() ?
                                     exceptionReferenced.get().getCause() : exceptionReferenced.get().getId());
            } else {
                //Link it, and start a new description
                String newId = idGenerator.newEntityId();
                description.setCause(newId);

                description = describeThrowable(currentNode, newId);
            }

            result.add(description);
            nextNode = nextNode.getCause();
        }

        return result;
    }

    /**
     * @return the maxStackTraceLength
     */
    public int getMaxStackTraceLength() {
        return maxStackTraceLength;
    }
}
