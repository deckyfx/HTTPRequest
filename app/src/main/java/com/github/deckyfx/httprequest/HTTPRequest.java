package com.github.deckyfx.httprequest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Created by decky on 9/8/16.
 */
public class HTTPRequest {
    public static final String REQUEST_CACHE_DB_NAME            = "httprequest.db";

    private Context                 mContext;
    private OkHttpClient            mHTTPClient;
    private HttpUrl                 mBaseURL;
    private ClearableCookieJar      mCookieStore;
    private ArrayList<Interceptor>  mApplicationInterceptors    = new ArrayList<Interceptor>(),
                                    mNetworkInterceptors        = new ArrayList<Interceptor>();
    protected DBHelper              DB;
    private Cache                   mRequestCache;
    private CacheControl            mCacheControl;
    private int                     mConnectTimeOut             = 30;
    private int                     mWriteTimeOut               = 30;
    private int                     mReadTimeOut                = 30;
    private HttpLoggingInterceptor  mLogInterceptor;
    private ChuckInterceptor        mChuckInterceptor;

    public HTTPRequest(Context context){
        this.mContext = context;
        this.setBaseURL("");
        this.DB = new DBHelper(this.mContext, DaoMaster.class, REQUEST_CACHE_DB_NAME);
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

    public void setBaseURL(HttpUrl baseURL) {
        if (baseURL == null) throw new NullPointerException("url == null");
        this.mBaseURL = baseURL;
    }

    public void setBaseURL(String baseURL){
        HttpUrl parsed = HttpUrl.parse(baseURL);
        if (parsed == null) throw new IllegalArgumentException("unexpected url: " + baseURL);
        this.setBaseURL(parsed);
    }

    public HttpUrl getBaseURL(){
        return this.mBaseURL;
    }

    public OkHttpClient getHTTPCLient(){
        return this.mHTTPClient;
    }

    public OkHttpClient getCopyHTTPCLient(){
        return this.mHTTPClient.newBuilder().build();
    }

    public void enableHTTPLogging(){
        this.mLogInterceptor = new HttpLoggingInterceptor();
        this.mLogInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
    }

    public void enableHTTPLogging(HttpLoggingInterceptor.Level level){
        this.mLogInterceptor = new HttpLoggingInterceptor();
        this.mLogInterceptor.setLevel(level);
    }

    public void enableChuckLogging(boolean notification){
        this.mChuckInterceptor = new ChuckInterceptor(this.mContext);
        this.mChuckInterceptor.showNotification(notification);
    }

    public void initCookieStore(){
        this.mCookieStore =  new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this.mContext));
    }

    public void initRequestCache(){
        this.mRequestCache =  new Cache(new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()), 10 * 1024 * 1024);
    }

    public void setRequestTimeOut(int timeOut) {
        this.mConnectTimeOut = timeOut;
        this.mWriteTimeOut = timeOut;
        this.mReadTimeOut = timeOut;
    }

    public void addApplicationInterceptor(Interceptor interceptor) {
        this.mApplicationInterceptors.add(interceptor);
    }

    public ArrayList<Interceptor> getApplicationInterceptors() {
        return this.mApplicationInterceptors;
    }

    public void removeApplicationInterceptors(int index) {
        this.mApplicationInterceptors.remove(index);
    }

    public void addNetworkInterceptor(Interceptor interceptor) {
        this.mNetworkInterceptors.add(interceptor);
    }

    public ArrayList<Interceptor> getNetworkInterceptors() {
        return this.mNetworkInterceptors;
    }

    public void removeNetworkInterceptors(int index) {
        this.mNetworkInterceptors.remove(index);
    }

    public void removeAllInterceptors() {
        this.mApplicationInterceptors.removeAll(this.mApplicationInterceptors);
        this.mNetworkInterceptors.removeAll(this.mNetworkInterceptors);
    }

    public void setCacheControl(CacheControl cacheControl){
        this.mCacheControl = cacheControl;
    }

    public OkHttpClient createHTTPClient(int timeout, ClearableCookieJar cookieJar, Cache cache,
                                         ArrayList<Interceptor> interceptors, ArrayList<Interceptor> networkInterceptor,
                                         Authenticator authenticator) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (cookieJar != null) {
            builder = builder.cookieJar(cookieJar);
        }
        if (timeout > 0) {
            builder = builder.connectTimeout(timeout, TimeUnit.SECONDS)
                    .writeTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS);
        }
        if (cache != null) {
            builder = builder.cache(new Cache(new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()), 10 * 1024 * 1024));  // 10 MiB
        }
        if (this.mLogInterceptor != null) {
            builder = builder.addInterceptor(this.mLogInterceptor);
        }
        if (this.mChuckInterceptor != null) {
            builder = builder.addInterceptor(this.mChuckInterceptor);
        }
        if (interceptors != null) {
            for (int i = 0; i < interceptors.size(); i++) {
                if (interceptors.get(i) != null) {
                    builder = builder.addInterceptor(interceptors.get(i));
                }
            }
        }
        if (networkInterceptor != null) {
            for (int i = 0; i < networkInterceptor.size(); i++) {
                if (networkInterceptor.get(i) != null) {
                    builder = builder.addNetworkInterceptor(networkInterceptor.get(i));
                }
            }
        }
        if (authenticator != null) {
            builder = builder.authenticator(authenticator);
        }
        return builder.build();
    }

    public void initHTTPCLient(){
        this.mHTTPClient = this.createHTTPClient(this.mConnectTimeOut, this.mCookieStore,
                this.mRequestCache, this.mApplicationInterceptors, this.mNetworkInterceptors,
                null);
    }

    public boolean isInitialized() {
        return (this.mHTTPClient != null);
    }

    public void clearCache() {
        try {
            this.mRequestCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mCookieStore.clear();
    }

    public void clearRequestCache() {
        this.DB.FlushAll();
    }

    public boolean isNetworkAvailable(Context ctx) {
        NetworkInfo localNetworkInfo = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (localNetworkInfo != null) && (localNetworkInfo.isConnected());
    }

    public void send(Request request) {
        Request.Builder builder = request.newBuilder();
        if (request.url() == null && request.path() != null && this.mBaseURL != null) builder.url(this.mBaseURL).path(request.path());
        if (this.DB != null) builder.dbHelper(this.DB);
        if (this.mCacheControl != null && request.cacheControl() == null) builder.cacheControl(this.mCacheControl);
        request = builder.build();

        okhttp3.Request req = new okhttp3.Request.Builder()
            .url(request.url())
            .cacheControl(request.cacheControl())
            .tag(request.tag())
            .method(request.method(), request.body())
            .headers(request.headers())
            .build();

        Call call               = this.mHTTPClient.newCall(req);
        request.onStart();
        if (!this.isNetworkAvailable(request.context())) {
            request.onNetworkError();
            return;
        }
        call.enqueue(request);
    }

    public void cancelAllRequest(Context ctx){
        // Should watch this two line bellow! a dangerous method
        // Likely will trigger crash when type for suggestion search
        this.mHTTPClient.dispatcher().cancelAll();
        for (Call call : this.mHTTPClient.dispatcher().queuedCalls()) {
            call.cancel();
        }
        for (Call call : this.mHTTPClient.dispatcher().runningCalls()) {
            call.cancel();
        }
    }
}
