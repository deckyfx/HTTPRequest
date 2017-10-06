package com.github.deckyfx.httprequest;

import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import com.github.deckyfx.httprequest.dao.DaoMaster;
import com.github.deckyfx.logging.HttpLoggingInterceptor;
import com.github.deckyfx.persistentcookiejar.ClearableCookieJar;
import com.github.deckyfx.persistentcookiejar.PersistentCookieJar;
import com.github.deckyfx.persistentcookiejar.cache.SetCookieCache;
import com.github.deckyfx.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Created by decky on 9/8/16.
 */
public class HTTPRequest {

    public static class Method {
        public static final String GET       = "GET";
        public static final String POST      = "POST";
        public static final String PUT       = "PUT";
        public static final String DELETE    = "DELETE";
    }

    public static class ErrorString {
        public static final String NO_ACTIVE_INTERNET           = "No active internet available";
        public static final String REQUEST_ERROR                = "Request Error";
        public static final String FAILED_RESPONSE              = "Server return failed response";
        public static final String NULL_CONTENTS                = "Server return null contents";
        public static final String REQUEST_FAILED               = "Request failed";
        public static final String ERROR_LOADING_DATA           = "Error loading data";
        public static final String REQUEST_TIMEOUT              = "Request timeout";
        public static final String CANNOT_CONNECT_TO_INTERNET   = "Can not connect to server";
    }

    public static final String REQUEST_CACHE_DB_NAME            = "httprequest.db";

    private Context                 mContext;
    private OkHttpClient            mHTTPClient;
    private String                  mBaseURL;
    private ClearableCookieJar      mCookieStore;
    private ArrayList<Interceptor>  mApplicationInterceptors    = new ArrayList<Interceptor>(),
                                    mNetworkInterceptors        = new ArrayList<Interceptor>();
    protected DBHelper              DB;
    private Cache                   mRequestCache;
    private int                     mConnectTimeOut             = 30;
    private int                     mWriteTimeOut               = 30;
    private int                     mReadTimeOut                = 30;
    private HttpLoggingInterceptor  mLogInterceptor             = new HttpLoggingInterceptor();;

    public HTTPRequest(Context context){
        this.mContext = context;
        this.setBaseURL("");
        this.DB = new DBHelper(this.mContext, DaoMaster.class, REQUEST_CACHE_DB_NAME);
        this.mLogInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
    }

    public HashMap<String, Cookie> getCookies(){
        HashMap<String, Cookie> result = new HashMap<String, Cookie>();
        URI uri = null;
        try {
            List<Cookie> cookielist = this.mCookieStore.loadForRequest(HttpUrl.parse(this.mBaseURL));
            uri = new URI(this.mBaseURL);
            String domain = uri.getHost();
            domain = domain.startsWith("www.") ? domain.substring(4) : domain;
            for (Cookie cookie : cookielist) {
                if (cookie.domain().equals(domain)) {
                    result.put(cookie.name(), cookie);
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void setBaseURL(String url){
        this.mBaseURL = url;
    }

    public String getBaseURL(){
        return this.mBaseURL;
    }

    public OkHttpClient getHTTPCLient(){
        return this.mHTTPClient;
    }

    public OkHttpClient getCopyHTTPCLient(){
        return this.mHTTPClient.newBuilder().build();
    }

    public boolean isNetworkAvailable(Context ctx) {
        NetworkInfo localNetworkInfo = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (localNetworkInfo != null) && (localNetworkInfo.isConnected());
    }

    public void send(RequestCall rcall) {
        rcall.setBaseUrl(this.mBaseURL);

        okhttp3.Request request     = null;
        if (!rcall.hasContentTypeHeader()) {
            rcall.addContentTypeHeader().addContentLengthHeader();
        }
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(rcall.getUrl())
                .cacheControl(new CacheControl.Builder().noCache().build())
                .tag(rcall.getRequestId())
                .method(rcall.getMethod(), rcall.getBody());
        for (Map.Entry<String, Object> header : rcall.getHeaders().entrySet()) {
            String param_value = "";
            if (header.getValue() != null) {
                param_value = header.getValue().toString();
            }
            builder.addHeader(header.getKey(), param_value);
        }
        request                 = builder.build();
        Call call               = this.mHTTPClient.newCall(request);
        rcall.setCall(call);
        rcall.setDBHelper(this.DB);
        rcall.onStart();
        if (!this.isNetworkAvailable(rcall.getContext())) {
            rcall.onNetworkError();
            return;
        }
        call.enqueue(rcall);
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
        builder = builder.addInterceptor(this.mLogInterceptor);
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

    public void setLogLevel(HttpLoggingInterceptor.Level level) {
        this.mLogInterceptor.setLevel(level);
    }

    public void clearRequestCache() {
        this.DB.FlushAll();
    }

    public static class ParamHeader {
        public String name      = "";
        public Object value     = "";
    }
}
