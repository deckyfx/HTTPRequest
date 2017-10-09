package com.github.deckyfx.httprequest;

import com.github.deckyfx.okhttp3.Response;

/**
 * Created by decky on 2/2/17.
 */
public interface RequestHandler {
    void onHTTPRequestStart(Request request);
    void onHTTPRequestFinish(Request request);
    void onHTTPRequestSuccess(Request request, Response response, String responseBody);
    void onHTTPRequestFailure(Request request, Throwable error);
    void onHTTPRequestRescue(Request request, String recoveredResponse);
    void onHTTPRequestNetworkError(Request request);
}
