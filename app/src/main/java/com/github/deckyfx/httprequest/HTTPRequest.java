package com.github.deckyfx.httprequest;

import android.content.Context;

import com.github.deckyfx.httprequest.dao.DaoMaster;
import com.github.deckyfx.logging.HttpLoggingInterceptor;
import com.github.deckyfx.logging.chuck.ChuckInterceptor;
import com.github.deckyfx.persistentcookiejar.ClearableCookieJar;
import com.github.deckyfx.persistentcookiejar.PersistentCookieJar;
import com.github.deckyfx.persistentcookiejar.cache.SetCookieCache;
import com.github.deckyfx.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Created by decky on 9/8/16.
 */
public class HTTPRequest extends OkHttpClient {
    protected static final String REQUEST_CACHE_DB_NAME            = "httprequest.db";

    private DBHelper                DB;
    private HTTPClient              mDefaultClient;
    private HttpUrl                 mBaseURL;
    private ClearableCookieJar      mCookieStore;
    private ArrayList<Interceptor>  mApplicationInterceptors    = new ArrayList<Interceptor>(),
            mNetworkInterceptors        = new ArrayList<Interceptor>();
    private Cache                   mRequestCache;
    private CacheControl            mCacheControl;
    private int                     mConnectTimeOut             = 30;
    private int                     mWriteTimeOut               = 30;
    private int                     mReadTimeOut                = 30;
    private HttpLoggingInterceptor  mLogInterceptor;
    private ChuckInterceptor        mChuckInterceptor;
    private Authenticator           mAuthenticator;

    public HTTPRequest() {

    }

    public HTTPRequest setupDBCache(Context ctx) {
        this.DB = new DBHelper(ctx, DaoMaster.class, REQUEST_CACHE_DB_NAME);
        this.mDefaultClient.newClientBuilder();
        return this;
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

    public HTTPRequest setBaseURL(HttpUrl baseURL) {
        if (baseURL == null) throw new NullPointerException("url == null");
        this.mBaseURL = baseURL;
        return this;
    }

    public HTTPRequest setBaseURL(String baseURL){
        HttpUrl parsed = HttpUrl.parse(baseURL);
        if (parsed == null) throw new IllegalArgumentException("unexpected url: " + baseURL);
        this.setBaseURL(parsed);
        return this;
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

    public HTTPRequest enableHTTPLogging(){
        this.mLogInterceptor = new HttpLoggingInterceptor();
        this.mLogInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return this;
    }

    public HTTPRequest enableHTTPLogging(HttpLoggingInterceptor.Level level){
        this.mLogInterceptor = new HttpLoggingInterceptor();
        this.mLogInterceptor.setLevel(level);
        return this;
    }

    public HTTPRequest enableChuckLogging(Context ctx, boolean notification){
        this.mChuckInterceptor = new ChuckInterceptor(ctx);
        this.mChuckInterceptor.showNotification(notification);
        return this;
    }

    public HTTPRequest initCookieStore(Context ctx){
        this.mCookieStore =  new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(ctx));
        return this;
    }

    public HTTPRequest initRequestCache(){
        this.mRequestCache =  new Cache(new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()), 10 * 1024 * 1024);
        return this;
    }

    public HTTPRequest setRequestTimeOut(int timeOut) {
        this.mConnectTimeOut = timeOut;
        this.mWriteTimeOut = timeOut;
        this.mReadTimeOut = timeOut;
        return this;
    }

    public HTTPRequest addApplicationInterceptor(Interceptor interceptor) {
        this.mApplicationInterceptors.add(interceptor);
        return this;
    }

    public ArrayList<Interceptor> getApplicationInterceptors() {
        return this.mApplicationInterceptors;
    }

    public HTTPRequest removeApplicationInterceptors(int index) {
        this.mApplicationInterceptors.remove(index);
        return this;
    }

    public HTTPRequest addNetworkInterceptor(Interceptor interceptor) {
        this.mNetworkInterceptors.add(interceptor);
        return this;
    }

    public ArrayList<Interceptor> getNetworkInterceptors() {
        return this.mNetworkInterceptors;
    }

    public HTTPRequest removeNetworkInterceptors(int index) {
        this.mNetworkInterceptors.remove(index);
        return this;
    }

    public HTTPRequest removeAllInterceptors() {
        this.mApplicationInterceptors.removeAll(this.mApplicationInterceptors);
        this.mNetworkInterceptors.removeAll(this.mNetworkInterceptors);
        return this;
    }

    public HTTPRequest setCacheControl(CacheControl cacheControl){
        this.mCacheControl = cacheControl;
        return this;
    }

    public HTTPRequest clearCache() {
        try {
            this.mRequestCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mCookieStore.clear();
        return this;
    }

    public HTTPRequest clearRequestCache() {
        this.DB.FlushAll();
        return this;
    }

    public HTTPRequest initDefaultClient() {
        HTTPClient.ClientBuilder ClientBuilder = new HTTPClient.ClientBuilder();
        if (this.mCookieStore != null) {
            ClientBuilder.getBuilder().cookieJar(this.mCookieStore);
        }
        if (this.mConnectTimeOut > 0) {
            ClientBuilder.getBuilder().connectTimeout(this.mConnectTimeOut, TimeUnit.SECONDS)
                    .writeTimeout(this.mConnectTimeOut, TimeUnit.SECONDS)
                    .readTimeout(this.mConnectTimeOut, TimeUnit.SECONDS);
        }
        if (this.mRequestCache != null) {
            ClientBuilder.getBuilder().cache(new Cache(new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()), 10 * 1024 * 1024));  // 10 MiB
        }
        if (this.mLogInterceptor != null) {
            ClientBuilder.getBuilder().addInterceptor(this.mLogInterceptor);
        }
        if (this.mChuckInterceptor != null) {
            ClientBuilder.getBuilder().addInterceptor(this.mChuckInterceptor);
        }
        if (this.mApplicationInterceptors != null) {
            for (int i = 0; i < this.mApplicationInterceptors.size(); i++) {
                if (this.mApplicationInterceptors.get(i) != null) {
                    ClientBuilder.getBuilder().addInterceptor(this.mApplicationInterceptors.get(i));
                }
            }
        }
        if (this.mNetworkInterceptors != null) {
            for (int i = 0; i < this.mNetworkInterceptors.size(); i++) {
                if (this.mNetworkInterceptors.get(i) != null) {
                    ClientBuilder.getBuilder().addNetworkInterceptor(this.mNetworkInterceptors.get(i));
                }
            }
        }
        if (this.mAuthenticator != null) {
            ClientBuilder.getBuilder().authenticator(this.mAuthenticator);
        }
        ClientBuilder.setBaseURL(this.getBaseURL());
        ClientBuilder.setCacheControl(this.getCacheControl());
        ClientBuilder.setCookieStore(this.getCookieStore());
        ClientBuilder.setDBCache(this.getDBCache());
        this.mDefaultClient = new HTTPClient(ClientBuilder.build());
        return this;
    }

    public HTTPClient getDefaultClient() {
        return this.mDefaultClient;
    }

    public void send(Request request) {
        this.getDefaultClient().send(request);
    }

    public void cancelRequests(){
        this.getDefaultClient().cancelRequests();
    }

    public boolean isNetworkAvailable(Context ctx) {
        return this.getDefaultClient().isNetworkAvailable(ctx);
    }

    public HTTPClient newClient() {
        return new HTTPClient(this.mDefaultClient.newClientBuilder());
    }

    public HTTPClient.ClientBuilder newClientBuilder() {
        return this.mDefaultClient.newClientBuilder();
    }
}
