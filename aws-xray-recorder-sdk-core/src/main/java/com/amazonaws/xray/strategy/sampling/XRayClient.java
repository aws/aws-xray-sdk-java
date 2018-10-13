package com.amazonaws.xray.strategy.sampling;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.xray.AWSXRayClientBuilder;
import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.xray.config.DaemonConfiguration;

public final class XRayClient {

    private static final AWSCredentialsProvider ANONYMOUS_CREDENTIALS = new AWSStaticCredentialsProvider(
            new AnonymousAWSCredentials());
    private static final String DUMMY_REGION = "us-west-1"; // Ignored because we use anonymous credentials
    private static final int TIME_OUT = 2000; // Milliseconds
    private XRayClient() {}

    public static AWSXRay newClient() {
        DaemonConfiguration config = new DaemonConfiguration();

        ClientConfiguration clientConfig = new ClientConfiguration()
                .withRequestTimeout(TIME_OUT);

        AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(
                config.getEndpointForTCPConnection(),
                DUMMY_REGION
        );

        return AWSXRayClientBuilder.standard()
                .withEndpointConfiguration(endpointConfig)
                .withClientConfiguration(clientConfig)
                .withCredentials(ANONYMOUS_CREDENTIALS) // This will entirely skip signing too
                .build();

    }
}
