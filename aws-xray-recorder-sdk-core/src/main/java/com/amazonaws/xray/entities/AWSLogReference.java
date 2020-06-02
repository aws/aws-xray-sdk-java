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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 * Represents a link between a trace segment and supporting CloudWatch logs.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AWSLogReference {

    private String logGroup;
    private String arn;

    /**
     * Returns the log group name associated with the segment.
     * @return
     */
    public String getLogGroup() {
        return logGroup;
    }

    /**
     * Set the log group for this reference.
     * @param logGroup
     */
    public void setLogGroup(final String logGroup) {
        this.logGroup = logGroup;
    }

    /**
     * Returns the ARN of the log group associated with this reference, or null if not provided by the AWS Runtime.
     * @return
     */
    public String getArn() {
        return arn;
    }

    /**
     * Set the ARN for this reference.
     * @param arn
     */
    public void setArn(final String arn) {
        this.arn = arn;
    }

    @Override
    /**
     * Compares ARN and log group between references to determine equality.
     * @param reference
     * @return
     */
    public boolean equals(Object o) {
        if (!(o instanceof AWSLogReference)) { return false; }
        AWSLogReference reference = (AWSLogReference) o;
        return (Objects.equals(getArn(), reference.getArn()) && Objects.equals(getLogGroup(), reference.getLogGroup()));
    }

    @Override
    /**
     * Generates unique hash for each LogReference object. Used to check equality in Sets.
     */
    public int hashCode() {
        if (arn == null && logGroup == null) {
            return "".hashCode();
        } else if (arn == null) {
            return logGroup.hashCode();
        } else {
            return arn.hashCode();
        }
    }
}
