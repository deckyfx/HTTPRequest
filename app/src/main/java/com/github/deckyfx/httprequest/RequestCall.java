package com.github.deckyfx.httprequest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by decky on 10/5/17.
 */

public class RequestCall implements Callback {
    private Context ctx                     = null;
    private String url                      = "";
    private String baseURL                  = "";
    private String method                   = HTTPRequest.Method.GET;
    private RequestBody body                = null;
    private Map<String, Object> params      = new HashMap<String, Object>();
    private Map<String, Object> headers     = new HashMap<String, Object>();
    private int requestId                   = -1;
    private boolean containFile             = false;
    private long contentLength              = 0;
    private Call call;
    private DBHelper db;
    private boolean isFinished;
    private RequestHandler requestHandler   = new RequestHandler() {
        @Override
        public void onHTTPRequestStart(RequestCall callback) {
            // Do Nothing
        }

        @Override
        public void onHTTPRequestFinish(RequestCall callback) {
            // Do Nothing
        }

        @Override
        public void onHTTPRequestSuccess(RequestCall callback, Response response, String responseBody) {
            // Do Nothing
        }

        @Override
        public void onHTTPRequestFailure(RequestCall callback, Throwable error) {
            // Do Nothing
        }

        @Override
        public void onHTTPRequestRescue(RequestCall callback, String recoveredResponse) {
            // Do Nothing
        }

        @Override
        public void onHTTPRequestNetworkError(RequestCall callback) {
            // Do Nothing
        }
    };

    public RequestCall(){

    }

    public RequestCall(Context ctx){
        this.setContext(ctx);
    }

    public RequestCall(Context ctx, String url){
        this.setContext(ctx).setUrl(url);
    }

    public RequestCall setContext(Context ctx){
        this.ctx = ctx;
        return this;
    }

    public RequestCall setUrl(String url){
        this.url = url;
        return this;
    }

    public RequestCall setBaseUrl(String baseURL){
        this.baseURL = baseURL;
        return this;
    }

    public RequestCall setMethod(String method){
        this.method = method;
        return this;
    }

    public RequestCall setRequestId(int requestId){
        this.requestId = requestId;
        return this;
    }

    public RequestCall setParams(Map<String, Object> params){
        this.params = new HashMap<>();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            this.addParam(param.getKey(), param.getValue());
        }
        return this;
    }

    public RequestCall addParam(String key, Object value) {
        this.params.put(key, value);
        if (value instanceof File) {
            this.containFile = true;
        }
        return this;
    }

    public RequestCall setHeaders(Map<String, Object> headers){
        this.headers = headers;
        return this;
    }

    public RequestCall addHeader(String key, Object value) {
        this.headers.put(key, value);
        return this;
    }

    public RequestCall addBasicAuthHeader(String login, String password) {
        this.params.put("Authorization", Credentials.basic(login, password));
        return this;
    }

    public RequestCall addAuthorizationHeader(String auth, String token) {
        this.params.put("Authorization", auth + " " + token);
        return this;
    }

    public RequestCall setRequestHandler(RequestHandler requestHandler){
        this.requestHandler = requestHandler;
        return this;
    }

    public RequestCall setBody(RequestBody body) {
        this.body = body;
        return this;
    }

    public RequestCall setDBHelper(DBHelper db) {
        this.db = db;
        return this;
    }

    public RequestCall addContentTypeHeader() {
        this.headers.put("Content-Type",    this.body.contentType().toString());
        return this;
    }

    public RequestCall addContentLengthHeader() {
        this.headers.put("Content-Length",  Long.toString(this.getContentLength()));
        return this;
    }

    public RequestCall setCall(Call call) {
        this.call = call;
        return this;
    }



    public Context getContext(){
        return this.ctx;
    }

    public String getMethod(){
        return this.method.toUpperCase(Locale.getDefault());
    }

    public String getUrl(){
        return this.buildURL();
    }

    public int getRequestId(){
        return this.requestId;
    }

    public Map<String, Object> getHeaders(){
        return this.headers;
    }

    public long getContentLength() {
        try {
            this.contentLength = this.body.contentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.contentLength;
    }

    public boolean hasContentTypeHeader() {
        return (this.headers.get("Content-Type") != null);
    }

    public RequestBody getBody() {
        if (this.body == null) {
            this.buildBody();
        }
        return this.body;
    }

    private String getAbsoluteUrl(String path) {
        if (path.contains("http://") || path.contains("https://")) {
            return path;
        } else {
            String pathFirstChar = "";
            String baseLastChar = "";
            if (path.length() > 0) {
                pathFirstChar = path.substring(0, 1);
            }
            if (this.baseURL.length() > 0) {
                baseLastChar = this.baseURL.substring(this.baseURL.length() - 1);
            }
            String prefix = "";
            if (!baseLastChar.equals("/") && !pathFirstChar.equals("/")) {
                prefix = "/";
            } else if (baseLastChar.equals("/") && pathFirstChar.equals("/")) {
                path = path.substring(path.indexOf("/"));
            }
            return this.baseURL + prefix + path;
        }
    }

    private String buildURL() {
        if (!Patterns.WEB_URL.matcher(this.url).matches()) {
            this.url = this.getAbsoluteUrl(this.url);
        }
        if (this.method.equals(HTTPRequest.Method.GET)) {
            HttpUrl parsed_url = HttpUrl.parse(url);
            HttpUrl.Builder urlBuilder;
            if (parsed_url != null) {
                urlBuilder = parsed_url.newBuilder();
            } else {
                urlBuilder = new HttpUrl.Builder();
            }
            for (Map.Entry<String, Object> param : this.params.entrySet()) {
                String param_value = "";
                if (param.getValue() != null) {
                    param_value = param.getValue().toString();
                }
                urlBuilder.addQueryParameter(param.getKey(), param_value);
            }
            if (parsed_url != null) {
                return urlBuilder.build().toString();
            }
        }
        return this.url;
    }

    private void buildBody() {
        if (this.containFile) {
            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
            bodyBuilder.setType(MultipartBody.FORM);
            for (Map.Entry<String, Object> param : this.params.entrySet()) {
                if (param.getValue() instanceof File) {
                    File f = (File) param.getValue();
                    String mimeType = null;
                    Uri uri = Uri.fromFile(f);
                    if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                        ContentResolver cR = this.ctx.getContentResolver();
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
                this.body = bodyBuilder.build();
            }
        } else {
            FormBody.Builder bodyBuilder = new FormBody.Builder();
            for (Map.Entry<String, Object> param : this.params.entrySet()) {
                String param_value = "";
                if (param.getValue() != null) {
                    param_value = param.getValue().toString();
                }
                bodyBuilder.add(param.getKey(), param_value);
            }
            this.body = bodyBuilder.build();
        }
    }

    public DBHelper getDBHelper(){
        return this.db;
    }

    public Call getCall() {
        return this.call;
    }

    public Request getRequest() {
        return this.call.request();
    }

    public boolean isFinished() {
        return this.isFinished;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        this.call = call;
        String errorMessage = e.getMessage();
        if (e != null) {
            if (errorMessage.equals(HTTPRequest.ErrorString.REQUEST_FAILED)) {
                // Skip general error content if there is previous error
                return;
            } else if (errorMessage.equals(HTTPRequest.ErrorString.NULL_CONTENTS)) {
                // Skip null contents error content if there is previous error
                return;
            } else if (e instanceof java.net.SocketTimeoutException) {
                errorMessage = HTTPRequest.ErrorString.REQUEST_TIMEOUT;
            } else {

            }
        }
        String param_str = (new JSONObject(this.params)).toString();
        String url = call.request().url().toString();
        String responseMessage = "";

        this.onFinish();
        this.onFail(new Exception(errorMessage));
        if (this.db != null) {
            responseMessage = this.db.loadResponseFromCache(url, call.request().method(), param_str);
        }
        if (responseMessage.length() > 0) {
            this.onRescue(responseMessage);
        }
    }

    @Override
    public void onResponse(Call call, okhttp3.Response response) throws IOException {
        this.call = call;
        byte[] response_bytes = new byte[0];
        try {
            response_bytes = response.body().bytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int request_code = response.code();
        String url = call.request().url().toString();
        String message = response.message();
        String param_str = (new JSONObject(this.params)).toString();
        String errorMessage = "";
        String responMessage = "";
        if (response_bytes != null) {
            responMessage = new String(response_bytes);
        }
        if ((request_code != 200 && request_code != 230)) {
            if (responMessage.length() == 0) {
                errorMessage = HTTPRequest.ErrorString.NULL_CONTENTS;
                if (response.message() != null) {
                    if (response.message().length() != 0) {
                        errorMessage = response.message();
                    }
                }
            } else {
                errorMessage = responMessage;
            }
        } else {
            if (responMessage.length() == 0) {
                errorMessage = HTTPRequest.ErrorString.NULL_CONTENTS;
            }
        }
        if (errorMessage.length() > 0) {
            this.onFinish();
            this.onFail(new Exception(errorMessage));
            if (this.db != null) {
                responMessage = this.db.loadResponseFromCache(url, call.request().method(), param_str);
            }
            if (responMessage.length() > 0) {
                this.onRescue(responMessage);
            }
        } else {
            this.onFinish();
            if (this.db != null) {
                this.db.saveResponseToCache(url, call.request().method(), param_str, responMessage);
            }
            this.onSuccess(response, responMessage);
        }
    }

    protected void onStart() {
        final RequestCall me = this;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestStart(me);
                }
            });
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    requestHandler.onHTTPRequestStart(me);
                }
            };
            thread.start();
        }
    }

    protected void onFinish() {
        final RequestCall me = this;
        this.isFinished = true;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestFinish(me);
                }
            });
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    requestHandler.onHTTPRequestFinish(me);
                }
            };
            thread.start();
        }
    }

    protected void onSuccess(final okhttp3.Response response, final String responMessage) {
        final RequestCall me = this;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestSuccess(me, new Response(response), responMessage);
                }
            });
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    requestHandler.onHTTPRequestSuccess(me, new Response(response), responMessage);
                }
            };
            thread.start();
        }
    }

    protected void onFail(final Throwable error) {
        final RequestCall me = this;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestFailure(me, error);
                }
            });
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    requestHandler.onHTTPRequestFailure(me, error);
                }
            };
            thread.start();
        }
    }

    protected void onRescue(final String recoveredResponse) {
        final RequestCall me = this;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestRescue(me, recoveredResponse);
                }
            });
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    requestHandler.onHTTPRequestRescue(me, recoveredResponse);
                }
            };
            thread.start();
        }
    }

    protected void onNetworkError() {
        final RequestCall me = this;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestNetworkError(me);
                }
            });
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    requestHandler.onHTTPRequestNetworkError(me);
                }
            };
            thread.start();
        }
    }
}
