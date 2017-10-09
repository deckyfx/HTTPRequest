package com.github.deckyfx.httprequest;

import com.github.deckyfx.okhttp3.Response;

/**
 * Created by decky on 2/2/17.
 */
public interface RequestHandler {
    void onHTTPRequestStart(Request callback);
    void onHTTPRequestFinish(Request callback);
    void onHTTPRequestSuccess(Request callback, Response response, String responseBody);
    void onHTTPRequestFailure(Request callback, Throwable error);
    void onHTTPRequestRescue(Request callback, String recoveredResponse);
    void onHTTPRequestNetworkError(Request callback);
}
