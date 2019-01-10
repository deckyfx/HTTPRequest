package com.github.deckyfx.httprequest;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Response;

/**
 * Created by decky on 2/2/17.
 */
public interface ClientListener {
    public interface Basic {
        public void onHTTPRequestStart(Request request);

        public void onHTTPRequestFinish(Request request);

        public void onHTTPRequestSuccess(Request request, Response response, String responseBody);

        public void onHTTPRequestFailure(Request request, Throwable error);
    }

    public interface Extended extends Basic {
        public void onHTTPRequestRescue(Request request, String recoveredResponse);

        public void onHTTPRequestNetworkError(Request request);
    }

    public interface Complete extends Basic {
        public void callStart(Call call);

        public void dnsStart(Call call, String domainName);

        public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList);

        public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy);

        public void secureConnectStart(Call call);

        public void secureConnectEnd(Call call, Handshake handshake);

        public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol);

        public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe);

        public void connectionAcquired(Call call, Connection connection);

        public void connectionReleased(Call call, Connection connection);

        public void requestHeadersStart(Call call);

        public void requestHeadersEnd(Call call, Request request);

        public void requestBodyStart(Call call);

        public void requestBodyEnd(Call call, long byteCount);

        public void responseHeadersStart(Call call);

        public void responseHeadersEnd(Call call, Response response);

        public void responseBodyStart(Call call);

        public void responseBodyEnd(Call call, long byteCount);

        public void callEnd(Call call);

        public void callFailed(Call call, IOException ioe);
    }
}