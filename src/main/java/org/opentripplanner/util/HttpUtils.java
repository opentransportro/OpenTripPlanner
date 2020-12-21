package org.opentripplanner.util;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class HttpUtils {
    
    private static final long TIMEOUT_CONNECTION = 5000;
    private static final int TIMEOUT_SOCKET = 5000;
    public enum Method { GET, POST };

    public static InputStream getData(URI uri) throws IOException {
        return getData(uri, Method.GET, null, null);
    }
    public static InputStream getData(URI uri, Method method) throws IOException {
        return getData(uri, method, null, null);
    }


    public static InputStream getData(String uri) throws IOException {
        return getData(URI.create(uri), Method.GET);
    }
    public static InputStream getData(String uri, Method method) throws IOException {
        return getData(URI.create(uri), method);
    }


    public static InputStream getData(URI uri, Method method, String requestHeaderName, String requestHeaderValue, long timeout) throws IOException {
        HttpRequestBase httprequest = null;
        switch (method) {
            case GET:
                httprequest = new HttpGet(uri);
                break;
            case POST:
                httprequest = new HttpPost(uri);
                break;
        }

        if (httprequest == null) {
            return null;
        }

        if (requestHeaderValue != null) {
            httprequest.addHeader(requestHeaderName, requestHeaderValue);
        }
        HttpClient httpclient = getClient(timeout, timeout);
        HttpResponse response = httpclient.execute(httprequest);
        if (response.getStatusLine().getStatusCode() != 200)
            return null;

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }
    public static InputStream getData(URI uri, String requestHeaderName, String requestHeaderValue, long timeout) throws IOException {
        return getData(uri, Method.GET, requestHeaderName, requestHeaderValue, timeout);
    }


    public static InputStream getData(URI uri, Method method, String requestHeaderName, String requestHeaderValue) throws IOException {
        return getData(uri, method, requestHeaderName, requestHeaderValue, TIMEOUT_CONNECTION);
    }
    public static InputStream getData(URI uri, String requestHeaderName, String requestHeaderValue) throws IOException {
        return getData(uri, Method.GET, requestHeaderName, requestHeaderValue, TIMEOUT_CONNECTION);
    }

    public static void testUrl(String url) throws IOException {
        HttpHead head = new HttpHead(url);
        HttpClient httpclient = getClient();
        HttpResponse response = httpclient.execute(head);

        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() == 404) {
            throw new FileNotFoundException();
        }

        if (status.getStatusCode() != 200) {
            throw new RuntimeException("Could not get URL: " + status.getStatusCode() + ": "
                    + status.getReasonPhrase());
        }
    }
    
    private static HttpClient getClient() {
        return getClient(TIMEOUT_CONNECTION, TIMEOUT_SOCKET);
    }

    private static HttpClient getClient(long timeoutConnection, long timeoutSocket) {
        return HttpClientBuilder.create()
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout((int)timeoutSocket).build())
                .setConnectionTimeToLive(timeoutConnection, TimeUnit.MILLISECONDS)
                .build();
    }
}
