package com.github.deckyfx.httprequest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.github.deckyfx.persistentcookiejar.ClearableCookieJar;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class HTTPClient {

    private HttpUrl mBaseURL;
    private DBHelper  DB;
    private CacheControl mCacheControl;
    private ClearableCookieJar mCookieStore;

    private OkHttpClient client;

    public HTTPClient() {
        this(new HTTPClient.ClientBuilder());
    }

    public HTTPClient(HTTPClient client) {
        this(client.newClientBuilder());
    }

    public HTTPClient(OkHttpClient.Builder builder) {
        this(new HTTPClient.ClientBuilder(builder));
    }

    public HTTPClient(OkHttpClient client) {
        this(new HTTPClient.ClientBuilder(client.newBuilder()));
    }

    public HTTPClient(ClientBuilder clientBuilder) {
        this.setClientBuilder(clientBuilder);
    }

    public HTTPClient setClientBuilder(ClientBuilder clientBuilder) {
        this.mBaseURL       = clientBuilder.mBaseURL;
        this.mCacheControl  = clientBuilder.mCacheControl;
        this.mCookieStore   = clientBuilder.mCookieStore;
        this.DB             = clientBuilder.DB;
        this.client         = clientBuilder.getBuilder().build();
        return this;
    }

    public ClientBuilder newClientBuilder() {
        ClientBuilder clientbuilder = new ClientBuilder(this);
        return clientbuilder;
    }

    public static final class ClientBuilder {
        private OkHttpClient.Builder builder;
        private HttpUrl mBaseURL;
        private DBHelper DB;
        private CacheControl mCacheControl;
        private ClearableCookieJar mCookieStore;
        private ClientListener mClientistener;

        public ClientBuilder() {
            this.builder = new OkHttpClient.Builder();
        }

        ClientBuilder(HTTPClient client) {
            this.mBaseURL       = client.mBaseURL;
            this.mCacheControl  = client.mCacheControl;
            this.mCookieStore   = client.mCookieStore;
            this.DB             = client.DB;
            this.builder        = client.client.newBuilder();
        }

        ClientBuilder(ClientBuilder builder) {
            this.mBaseURL       = builder.mBaseURL;
            this.mCacheControl  = builder.mCacheControl;
            this.mCookieStore   = builder.mCookieStore;
            this.DB             = builder.DB;
            this.builder        = builder.getBuilder();
        }

        ClientBuilder(OkHttpClient client) {
            this.setBuilder(client.newBuilder());
        }

        ClientBuilder(OkHttpClient.Builder builder) {
            this.setBuilder(builder);
        }

        public OkHttpClient.Builder getBuilder() {
            return this.builder;
        }

        public ClientBuilder setBuilder(OkHttpClient.Builder builder) {
            this.builder = builder;
            return this;
        }

        public HTTPClient build() {
            this.builder.eventListener(new PrintingEventListener(this.mClientistener));
            return new HTTPClient(this);
        }

        public ClientBuilder setBaseURL(HttpUrl baseURL) {
            if (baseURL == null) throw new NullPointerException("url == null");
            this.mBaseURL = baseURL;
            return this;
        }

        public ClientBuilder setBaseURL(String baseURL){
            HttpUrl parsed = HttpUrl.parse(baseURL);
            if (parsed == null) throw new IllegalArgumentException("unexpected url: " + baseURL);
            this.setBaseURL(parsed);
            return this;
        }

        public ClientBuilder setCacheControl(CacheControl cacheControl){
            this.mCacheControl = cacheControl;
            return this;
        }

        public ClientBuilder setCookieStore(ClearableCookieJar cookieStore){
            this.mCookieStore = cookieStore;
            return this;
        }

        public ClientBuilder setDBCache(DBHelper dbhelper){
            this.DB = dbhelper;
            return this;
        }

        public ClientBuilder setClientListener(ClientListener listener){
            this.mClientistener = listener;
            return this;
        }
    }

    public void send(Request request) {
        Request.Builder builder = request.newBuilder();
        if (request.url() == null && request.path() != null && this.mBaseURL != null) builder.url(this.mBaseURL).path(request.path());
        if (this.DB != null) builder.dbHelper(this.DB);
        if (this.mCacheControl != null && request.cacheControl() == null) builder.cacheControl(this.mCacheControl);
        request = builder.build(true);

        // Final check url can not be empty
        if (request.url() == null) throw new NullPointerException("url == null");

        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(request.url())
                .cacheControl(request.cacheControl())
                .tag(request.tag())
                .method(request.method(), request.body())
                .headers(request.headers())
                .build();

        if (!request.validContext()) {
            return;
        }

        Call call               = this.client.newCall(req);
        request.onStart();
        if (!this.isNetworkAvailable(request.context())) {
            request.onNetworkError();
            return;
        }
        call.enqueue(request);
    }

    public void cancelRequests(){
        // Should watch this two line bellow! a dangerous method
        // Likely will trigger crash when type for suggestion search
        this.client.dispatcher().cancelAll();
        for (Call call : this.client.dispatcher().queuedCalls()) {
            call.cancel();
        }
        for (Call call : this.client.dispatcher().runningCalls()) {
            call.cancel();
        }
    }

    public boolean isNetworkAvailable(Context ctx) {
        NetworkInfo localNetworkInfo = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (localNetworkInfo != null) && (localNetworkInfo.isConnected());
    }

    public HashMap<String, Cookie> getCookies(){
        HashMap<String, Cookie> result = new HashMap<String, Cookie>();
        List<Cookie> cookielist = this.mCookieStore.loadForRequest(this.mBaseURL);
        URI uri = this.mBaseURL.uri();
        String domain = uri.getHost();
        domain = domain.startsWith("www.") ? domain.substring(4) : domain;
        for (Cookie cookie : cookielist) {
            if (cookie.domain().equals(domain)) {
                result.put(cookie.name(), cookie);
            }
        }
        return result;
    }

    public HttpUrl getBaseURL(){
        return this.mBaseURL;
    }

    public CacheControl getCacheControl(){
        return this.mCacheControl;
    }

    public ClearableCookieJar getCookieStore(){
        return this.mCookieStore;
    }

    public DBHelper getDBCache(){
        return this.DB;
    }
}
