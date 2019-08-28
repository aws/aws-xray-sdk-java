package com.amazonaws.xray.entities;

/**
 * Represents a link between a trace segment and supporting CloudWatch logs.
 *
 */
public class AWSLogReference {

    private String logGroup;
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

    /**
     * Compares ARN and log group between references to determine equality.
     * @param reference
     * @return
     */
    public boolean equals(AWSLogReference reference) {
        return (getLogGroup().equals(reference.getLogGroup()) && getArn().equals(reference.getArn()));
    }
}
