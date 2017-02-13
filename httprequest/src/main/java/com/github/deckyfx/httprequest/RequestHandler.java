package com.github.deckyfx.httprequest;

import okhttp3.Response;

/**
 * Created by decky on 2/2/17.
 */
public interface RequestHandler {
    void onHTTPRequestStart(RequestCallBack callback);
    void onHTTPRequestFinish(RequestCallBack callback);
    void onHTTPRequestSuccess(RequestCallBack callback, Response response, String responseBody);
    void onHTTPRequestFailure(RequestCallBack callback, Throwable error);
    void onHTTPRequestRescue(RequestCallBack callback, String recoveredResponse);
    void onHTTPRequestNetworkError(RequestCallBack callback);
}
