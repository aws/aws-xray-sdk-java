package com.amazonaws.xray.entities;

import java.util.Objects;

/**
 * Represents a link between a trace segment and supporting CloudWatch logs.
 *
 */
public class AWSLogReference {

    private String logGroup = null;
    private String arn = "UNDEFINED";

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
     * Returns the ARN of the log group associated with this reference, or UNDEFINED if not provided by the AWS Runtime.
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
        return (getLogGroup().equals(reference.getLogGroup()) && getArn().equals(reference.getArn()));
    }

    @Override
    /**
     * Generates unique hash for each LogReference object. Used to check equality in Sets.
     */
    public int hashCode() {
        if (arn == "UNDEFINED" && logGroup == null) {
            return Objects.hash("");
        } else if (arn == "UNDEFINED") {
            return Objects.hash(logGroup);
        } else {
            return Objects.hash(arn);
        }
    }
}
