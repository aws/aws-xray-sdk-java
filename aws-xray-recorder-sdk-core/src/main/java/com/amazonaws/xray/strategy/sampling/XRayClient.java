package com.amazonaws.xray.strategy.sampling;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.SignerFactory;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.xray.AWSXRayClientBuilder;
import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.xray.config.DaemonConfiguration;

public final class XRayClient {

    private static final String DUMMY_REGION = "us-west-1"; // Ignored because we use a No-op signer
    private static final int TIME_OUT = 2000; // Milliseconds
    private XRayClient() {}

    public static AWSXRay newClient() {
        DaemonConfiguration config = new DaemonConfiguration();

        ClientConfiguration clientConfig = new ClientConfiguration()
                .withSignerOverride(SignerFactory.NO_OP_SIGNER)
                .withRequestTimeout(TIME_OUT);

        AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(
                config.getEndpointForTCPConnection(),
                DUMMY_REGION
        );

        return AWSXRayClientBuilder.standard()
                .withEndpointConfiguration(endpointConfig)
                .withClientConfiguration(clientConfig)
                .build();

    }
}
