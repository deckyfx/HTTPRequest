package com.github.deckyfx.httprequest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.github.deckyfx.httprequest.dao.DaoMaster;

import org.json.JSONObject;

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
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

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

    private Context mContext;
    private OkHttpClient mHTTPClient;
    private String mBaseURL;
    private ClearableCookieJar mCookieStore;
    private DBHelper DB;
    private Cache mRequestCache;
    private int mConnectTimeOut = 30;
    private int mWriteTimeOut = 30;
    private int mReadTimeOut = 30;

    public HTTPRequest(Context context){
        this.mContext = context;
        this.setBaseURL("");
        this.DB = new DBHelper(this.mContext, DaoMaster.class, REQUEST_CACHE_DB_NAME);
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

    public String getAbsoluteUrl(String path) {
        if (path.contains("http://") || path.contains("https://")) {
            return path;
        } else {
            String pathFirstChar = "";
            String baseLastChar = "";
            if (path.length() > 0) {
                pathFirstChar = path.substring(0, 1);
            }
            if (this.mBaseURL.length() > 0) {
                baseLastChar = this.mBaseURL.substring(this.mBaseURL.length() - 1);
            }
            String prefix = "";
            if (!baseLastChar.equals("/") && !pathFirstChar.equals("/")) {
                prefix = "/";
            } else if (baseLastChar.equals("/") && pathFirstChar.equals("/")) {
                path = path.substring(path.indexOf("/"));
            }
            return this.mBaseURL + prefix + path;
        }
    }

    public void setBaseURL(String url){
        this.mBaseURL = url;
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

    public void send(Context ctx, String url) {
        this.send(ctx, url, null, null, null, 0, null);
    }

    public void send(Context ctx, String url, RequestHandler requestHandler) {
        this.send(ctx, url, null, null, null, 0, requestHandler);
    }

    public void send(Context ctx, String url, int requestId, RequestHandler requestHandler) {
        this.send(ctx, url, null, null, null, requestId, requestHandler);
    }

    public void send(Context ctx, String url, String method) {
        this.send(ctx, url, method, null, null, 0, null);
    }

    public void send(Context ctx, String url, String method, RequestHandler requestHandler) {
        this.send(ctx, url, method, null, null, 0, requestHandler);
    }

    public void send(Context ctx, String url, String method, int requestId, RequestHandler requestHandler) {
        this.send(ctx, url, method, null, null, requestId, requestHandler);
    }

    public void send(Context ctx, String url, String method, Map<String, Object> params) {
        this.send(ctx, url, method, params, null, 0, null);
    }

    public void send(Context ctx, String url, String method,  Map<String, Object> params, RequestHandler requestHandler) {
        this.send(ctx, url, method, params, null, 0, requestHandler);
    }

    public void send(Context ctx, String url, String method,  Map<String, Object> params, int requestId, RequestHandler requestHandler) {
        this.send(ctx, url, method, params, null, requestId, requestHandler);
    }

    public void send(Context ctx, String url, String method, Map<String, Object> params, Map<String, Object> headers) {
        this.send(ctx, url, method, params, headers, 0, null);
    }

    public void send(Context ctx, String url, String method, Map<String, Object> params, Map<String, Object> headers, RequestHandler requestHandler) {
        this.send(ctx, url, method, params, headers, 0, requestHandler);
    }

    public void send(Context ctx, String url, String method, Map<String, Object> params, Map<String, Object> headers, int requestId, RequestHandler requestHandler) {
        if (method == null) {
            method = HTTPRequest.Method.GET;
        }
        if (params == null) {
            params = new HashMap<String, Object>();
        }
        if (headers == null) {
            headers = new HashMap<String, Object>();
        }
        if (requestHandler == null) {
            requestHandler = new RequestHandler() {
                @Override
                public void onHTTPRequestStart(Call call) {

                }

                @Override
                public void onHTTPRequestFinish(Call call) {

                }

                @Override
                public void onHTTPRequestSuccess(Call call, Response response, String responseBody) {

                }

                @Override
                public void onHTTPRequestFailure(Call call, Throwable error) {

                }

                @Override
                public void onHTTPRequestRescue(Call call, String recoveredResponse) {

                }

                @Override
                public void onHTTPRequestNetworkError(Call call) {

                }
            };
        }
        method = method.toUpperCase(Locale.getDefault());
        RequestBody body = null;
        okhttp3.Request request = null;
        if (method.equals("GET") || method.equals("DELETE")) {
            url = this.buildURL(url, params);
        } else {
            url = this.buildURL(url, null);
            boolean containFile = false;
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (param.getValue() instanceof File) {
                    containFile = true;
                    break;
                }
            }

            if (containFile) {
                MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
                bodyBuilder.setType(MultipartBody.FORM);
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (param.getValue() instanceof File) {
                        File f = (File) param.getValue();
                        String mimeType = null;
                        Uri uri = Uri.fromFile(f);
                        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                            ContentResolver cR = this.mContext.getContentResolver();
                            mimeType = cR.getType(uri);
                        } else {
                            String extension = MimeTypeMap.getFileExtensionFromUrl(f.getAbsolutePath());
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                        }
                        bodyBuilder.addFormDataPart(param.getKey(), f.getName(), RequestBody.create(MediaType.parse(mimeType), f));
                    } else {
                        String param_value = "";
                        if (param.getValue() != null) {
                            param_value = param.getValue().toString();
                        }
                        bodyBuilder.addFormDataPart(param.getKey(), param_value);
                    }
                    body = bodyBuilder.build();
                }
            } else {
                FormBody.Builder bodyBuilder = new FormBody.Builder();
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    String param_value = "";
                    if (param.getValue() != null) {
                        param_value = param.getValue().toString();
                    }
                    bodyBuilder.add(param.getKey(), param_value);
                }
                body = bodyBuilder.build();
            }
            headers.put("Content-Type", body.contentType().toString());
            long contentLength = 0;
            try {
                contentLength = body.contentLength();
            } catch (IOException e) {
                e.printStackTrace();
            }
            headers.put("Content-Length",  Long.toString(contentLength));
        }
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(url)
                .cacheControl(new CacheControl.Builder().noCache().build())
                .tag(requestId)
                .method(method, body);
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            String param_value = "";
            if (header.getValue() != null) {
                param_value = header.getValue().toString();
            }
            builder.addHeader(header.getKey(), param_value);
        }
        request = builder.build();

        Call call = this.mHTTPClient.newCall(request);
        if (requestHandler != null) {
            this.onStart(ctx, requestHandler, call);
        }
        if (!this.isNetworkAvailable(ctx)) {
            if (requestHandler != null) {
                this.onNetworkError(ctx, requestHandler, call);
            }
            return;
        }
        call.enqueue(new RequestCallBack(this, ctx, call, requestHandler));
    }

    public Map<String, Object> addBasicAuthToParam(Map<String, Object> params, String login, String password) {
        if (params == null) {
            params = new HashMap<String, Object>();
        }
        params.put("Authorization", Credentials.basic(login, password));
        return params;
    }

    public Map<String, Object> addAuthToParam(Map<String, Object> params, String auth, String token) {
        if (params == null) {
            params = new HashMap<String, Object>();
        }
        params.put("Authorization", auth + " " + token);
        return params;
    }

    public String buildURL(String url, Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<String, Object>();
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            url = this.getAbsoluteUrl(url);
        }
        if (url.length() == 0) {
            return "";
        }
        HttpUrl parsed_url = HttpUrl.parse(url);
        HttpUrl.Builder urlBuilder;
        if (parsed_url != null) {
            urlBuilder = parsed_url.newBuilder();
        } else {
            urlBuilder = new HttpUrl.Builder();
        }
        for (Map.Entry<String, Object> param : params.entrySet()) {
            String param_value = "";
            if (param.getValue() != null) {
                param_value = param.getValue().toString();
            }
            urlBuilder.addQueryParameter(param.getKey(), param_value);
        }
        if (parsed_url != null) {
            return urlBuilder.build().toString();
        } else {
            return url;
        }
    }

    private HashMap<String, Object> parseRequestBodyToHashMap(FormBody formbody) {
        HashMap<String, Object> result = new HashMap<String, Object>();
        if (formbody != null) {
            for (int i = 0; i < formbody.size(); i++) {
                result.put(formbody.name(i), formbody.value(i));
            }
        }
        return result;
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

    public OkHttpClient createHTTPClient(int timeout, ClearableCookieJar cookieJar, Cache cache,
                                         ArrayList<Interceptor> interceptors, ArrayList<Interceptor> networkInterceptor,
                                         Authenticator authenticator) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
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
        builder = builder.addInterceptor(logging);
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
                this.mRequestCache, new ArrayList<Interceptor>(), new ArrayList<Interceptor>(),
                null);
    }

    public void clearCache() {
        try {
            this.mRequestCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mCookieStore.clear();
    }

    private void onStart(Context ctx, final RequestHandler requestHandler, final Call call) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestStart(call);
                }
            });
        } else {
            requestHandler.onHTTPRequestStart(call);
        }
    }

    private void onFinish(Context ctx, final RequestHandler requestHandler, final Call call) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestFinish(call);
                }
            });
        } else {
            requestHandler.onHTTPRequestFinish(call);
        }
    }

    private void onSuccess(Context ctx, final RequestHandler requestHandler, final Call call, final Response response, final String responMessage) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestSuccess(call, response, responMessage);
                }
            });
        } else {
            requestHandler.onHTTPRequestSuccess(call, response, responMessage);
        }
    }

    private void onFail(Context ctx, final RequestHandler requestHandler, final Call call, final Throwable error) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestFailure(call, error);
                }
            });
        } else {
            requestHandler.onHTTPRequestFailure(call, error);
        }
    }

    private void onRescue(Context ctx, final RequestHandler requestHandler, final Call call, final String recoveredResponse) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestRescue(call, recoveredResponse);
                }
            });
        } else {
            requestHandler.onHTTPRequestRescue(call, recoveredResponse);
        }
    }

    private void onNetworkError(Context ctx, final RequestHandler requestHandler, final Call call) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestNetworkError(call);
                }
            });
        }
    }

    public static class RequestCallBack implements Callback{
        private DBHelper db;
        private Context ctx;
        private RequestHandler requestHandler;
        private HTTPRequest request;
        private Call mCall;

        public RequestCallBack(HTTPRequest request, Context ctx, Call call, RequestHandler requestHandler) {
            this.ctx = ctx;
            this.request = request;
            this.db = request.DB;
            this.mCall = call;
            this.requestHandler = requestHandler;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            String errorMessage = e.getMessage();
            if (e != null) {
                if (errorMessage.equals(ErrorString.REQUEST_FAILED)) {
                    // Skip general error content if there is previous error
                    return;
                } else if (errorMessage.equals(ErrorString.NULL_CONTENTS)) {
                    // Skip null contents error content if there is previous error
                    return;
                } else if (e instanceof java.net.SocketTimeoutException) {
                    errorMessage = ErrorString.REQUEST_TIMEOUT;
                } else {

                }
            }
            HashMap<String, Object>params = this.request.parseRequestBodyToHashMap((FormBody) call.request().body());
            JSONObject json_param = new JSONObject(params);
            String url = call.request().url().toString();
            String responseMessage = "";

            this.request.onFinish(ctx, this.requestHandler, call);
            this.request.onFail(ctx, this.requestHandler, call, new Exception(errorMessage));
            if (this.db != null) {
                responseMessage = this.db.loadResponseFromCache(url, call.request().method(), json_param.toString());
            }
            if (responseMessage.length() > 0) {
                this.request.onRescue(ctx, this.requestHandler, call, responseMessage);
            }
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            byte[] response_bytes = new byte[0];
            try {
                response_bytes = response.body().bytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int request_code = response.code();
            String url = call.request().url().toString();
            String message = response.message();
            HashMap<String, Object>params = this.request.parseRequestBodyToHashMap((FormBody) call.request().body());
            JSONObject json_param = new JSONObject(params);

            String errorMessage = "";
            String responMessage = "";
            if (response_bytes != null) {
                responMessage = new String(response_bytes);
            }
            if ((request_code != 200 && request_code != 230)) {
                if (responMessage.length() == 0) {
                    errorMessage = ErrorString.NULL_CONTENTS;
                    if (response.message().length() != 0) {
                        errorMessage = response.message();
                    }
                } else {
                    errorMessage = responMessage;
                }
            } else {
                if (responMessage.length() == 0) {
                    errorMessage = ErrorString.NULL_CONTENTS;
                }
            }
            if (errorMessage.length() > 0) {
                this.request.onFinish(ctx, this.requestHandler, call);
                this.request.onFail(ctx, this.requestHandler, call,  new Exception(errorMessage));
                if (this.db != null) {
                    responMessage = this.db.loadResponseFromCache(url, call.request().method(), json_param.toString());
                }
                if (responMessage.length() > 0) {
                    this.request.onRescue(ctx, this.requestHandler, call, responMessage);
                }
            } else {
                this.request.onFinish(ctx, this.requestHandler, call);
                if (this.db != null) {
                    this.db.saveResponseToCache(url, call.request().method(), json_param.toString(), responMessage);
                }
                this.request.onSuccess(ctx, this.requestHandler, call, response, responMessage);
            }
        }
    }

    public interface RequestHandler {
        void onHTTPRequestStart(Call call);
        void onHTTPRequestFinish(Call call);
        void onHTTPRequestSuccess(Call call, Response response, String responseBody);
        void onHTTPRequestFailure(Call call, Throwable error);
        void onHTTPRequestRescue(Call call, String recoveredResponse);
        void onHTTPRequestNetworkError(Call call);
    }
}
