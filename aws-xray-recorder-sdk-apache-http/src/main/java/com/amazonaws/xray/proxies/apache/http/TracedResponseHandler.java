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

package com.amazonaws.xray.proxies.apache.http;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

/**
 * Wraps an instance of {@code org.apache.http.client.ResponseHandler} and adds response information to the current subsegment.
 *
 */
public class TracedResponseHandler<T> implements ResponseHandler<T> {

    private final ResponseHandler<T> wrappedHandler;

    public TracedResponseHandler(ResponseHandler<T> wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
    }

    public static void addResponseInformation(Subsegment subsegment, HttpResponse response) {
        if (null == subsegment) {
            return;
        }

        Map<String, Object> responseInformation = new HashMap<>();

        int responseCode = response.getStatusLine().getStatusCode();
        switch (responseCode / 100) {
            case 4:
                subsegment.setError(true);
                if (429 == responseCode) {
                    subsegment.setThrottle(true);
                }
                break;
            case 5:
                subsegment.setFault(true);
                break;
            default:
        }
        responseInformation.put("status", responseCode);

        if (null != response.getEntity()) {
            responseInformation.put("content_length", response.getEntity().getContentLength());
        }

        subsegment.putHttp("response", responseInformation);
    }

    @Override
    public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        T handled = wrappedHandler.handleResponse(response);
        Subsegment currentSubsegment = AWSXRay.getCurrentSubsegment();
        if (null != currentSubsegment) {
            TracedResponseHandler.addResponseInformation(currentSubsegment, response);
        }
        return handled;
    }

}
