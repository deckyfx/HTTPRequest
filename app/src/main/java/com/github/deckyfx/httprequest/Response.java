package com.github.deckyfx.httprequest;

/**
 * Created by decky on 9/25/17.
 */

public class Response {
    private okhttp3.Response response;

    public Response(okhttp3.Response r) {
        this.response = r;
    }
}
