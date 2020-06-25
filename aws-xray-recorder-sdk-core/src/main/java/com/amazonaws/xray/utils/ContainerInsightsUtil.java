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

package com.amazonaws.xray.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class for querying configuration information from ContainerInsights enabled Kubernetes clusters.
 *
 * @deprecated For internal use only.
 */
@Deprecated
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class ContainerInsightsUtil {

    private static final String K8S_CRED_FOLDER = "/var/run/secrets/kubernetes.io/serviceaccount";
    private static final String K8S_CRED_TOKEN_SUFFIX = "token";
    private static final String K8S_CRED_CERT_SUFFIX = "ca.crt";
    private static final String K8S_URL = "https://kubernetes.default.svc";
    private static final String CI_CONFIGMAP_PATH = "/api/v1/namespaces/amazon-cloudwatch/configmaps/cluster-info";
    private static final String AUTH_HEADER_NAME = "Authorization";
    private static final int HTTP_TIMEOUT = 5;

    private static final Log logger = LogFactory.getLog(ContainerInsightsUtil.class);

    /**
     * Return the cluster name from ContainerInsights configMap via the K8S API and the pod's system account.
     *
     * @return the name
     */
    @Nullable
    public static String getClusterName() {
        if (isK8s()) {
            CloseableHttpClient client = getHttpClient();
            HttpGet getRequest = new HttpGet(K8S_URL + CI_CONFIGMAP_PATH);

            String k8sCredentialHeader = getK8sCredentialHeader();
            if (k8sCredentialHeader != null) {
                getRequest.setHeader(AUTH_HEADER_NAME, k8sCredentialHeader);
            }

            try {
                CloseableHttpResponse response = client.execute(getRequest);

                try {
                    HttpEntity entity = response.getEntity();
                    String json = EntityUtils.toString(entity);

                    ObjectMapper mapper = new ObjectMapper();
                    String clusterName = mapper.readTree(json).at("/data/cluster.name").asText();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Container Insights Cluster Name: " + clusterName);
                    }

                    return clusterName;

                } catch (IOException e) {
                    logger.error("Error parsing response from Kubernetes", e);
                } finally {
                    response.close();
                }

                client.close();

            } catch (IOException  e) {
                logger.error("Error querying for Container Insights ConfigMap", e);
            }
        }

        return null;
    }

    private static CloseableHttpClient getHttpClient() {
        KeyStore k8sTrustStore = getK8sKeystore();
        try {
            if (k8sTrustStore != null) {
                TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(k8sTrustStore);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers != null) {
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, trustManagers, new SecureRandom());
                    return HttpClients.custom().setSSLContext(context).build();
                }
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.debug("Unable to create HTTP client with K8s CA certs, using default trust store.", e);
        }

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(HTTP_TIMEOUT * 1000)
                .setConnectionRequestTimeout(HTTP_TIMEOUT * 1000)
                .setSocketTimeout(HTTP_TIMEOUT * 1000).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    @Nullable
    private static KeyStore getK8sKeystore() {

        InputStream certificateFile = null;

        try {
            KeyStore k8sTrustStore = null;
            File caFile = Paths.get(K8S_CRED_FOLDER, K8S_CRED_CERT_SUFFIX).toFile();

            if (caFile.exists()) {
                k8sTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                k8sTrustStore.load(null, null);
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

                certificateFile =
                        new FileInputStream(caFile);
                Collection<? extends Certificate> certificates =
                        certificateFactory.generateCertificates(certificateFile);

                if (certificates.isEmpty()) {
                    throw new IllegalArgumentException("K8s cert file contained no certificates.");
                }

                for (Certificate certificate : certificates) {
                    k8sTrustStore.setCertificateEntry("k8sca", certificate);
                }
            } else {
                logger.debug("K8s CA Cert file does not exists.");
            }

            return k8sTrustStore;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            logger.warn("Unable to load K8s CA certificate.", e);
            return null;
        } finally {
            if (certificateFile != null) {
                try {
                    certificateFile.close();
                } catch (IOException e) {
                    logger.error("Can't close K8s CA certificate file.", e);
                }
            }
        }
    }

    @Nullable
    private static String getK8sCredentialHeader() {

        BufferedReader tokenReader = null;

        try {
            File tokenFile = Paths.get(K8S_CRED_FOLDER, K8S_CRED_TOKEN_SUFFIX).toFile();
            tokenReader = new BufferedReader(new InputStreamReader(new FileInputStream(tokenFile), StandardCharsets.UTF_8));
            return String.format("Bearer %s", tokenReader.readLine());
        } catch (IOException e) {
            logger.warn("Unable to read K8s credential file.", e);
        } finally {
            if (tokenReader != null) {
                try {
                    tokenReader.close();
                } catch (IOException e) {
                    logger.error("Can't close K8s credential file.", e);
                }
            }
        }
        return null;
    }

    public static boolean isK8s() {
        return Paths.get(K8S_CRED_FOLDER, K8S_CRED_TOKEN_SUFFIX).toFile().exists();
    }
}
