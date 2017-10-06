package com.github.deckyfx.httprequest;

/**
 * Created by decky on 2/2/17.
 */
public interface RequestHandler {
    void onHTTPRequestStart(RequestCall callback);
    void onHTTPRequestFinish(RequestCall callback);
    void onHTTPRequestSuccess(RequestCall callback, Response response, String responseBody);
    void onHTTPRequestFailure(RequestCall callback, Throwable error);
    void onHTTPRequestRescue(RequestCall callback, String recoveredResponse);
    void onHTTPRequestNetworkError(RequestCall callback);
}
