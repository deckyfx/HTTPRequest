/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.deckyfx.httprequest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Patterns;
import android.webkit.MimeTypeMap;

import com.github.deckyfx.greendao.annotation.NotNull;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;

/**
 * An HTTP request. Instances of this class are immutable if their {@link #body} is null or itself
 * immutable.
 */
public class Request implements Callback {
    private Context ctx                         = null;
    private HttpUrl url                         = null;
    private String path                         = null;
    private String method                       = "";
    private Headers headers                     = null;
    private @Nullable RequestBody body          = null;
    private Object tag                          = null;
    private DBHelper db                         = null;
    private Map<String, Object> params          = null;
    private volatile CacheControl cacheControl  = null; // Lazily initialized.
    private RequestListener requestHandler      = null;
    private Call call                           = null;
    private boolean isFinished                  = false;

    Request(Builder builder) {
        this.ctx                = builder.ctx;
        this.url                = builder.url;
        this.path               = builder.path;
        this.method             = builder.method;
        this.headers            = builder.headers.build();
        this.body               = builder.body;
        this.tag                = builder.tag != null ? builder.tag : this;
        this.db                 = builder.db;
        this.params             = builder.params;
        this.requestHandler     = builder.requestHandler;
        this.call               = builder.call;
        this.cacheControl       = builder.cacheControl;
    }

    public HttpUrl url() {
        return this.url;
    }

    public String method() {
        return this.method.toUpperCase(Locale.getDefault());
    }

    public Headers headers() {
        return this.headers;
    }

    public String header(String name) {
        return this.headers.get(name);
    }

    public List<String> headers(String name) {
        return this.headers.values(name);
    }

    public @Nullable
    RequestBody body() {
        return this.body;
    }

    public Object tag() {
        return this.tag;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Returns the cache control directives for this response. This is never null, even if this
     * response contains no {@code Cache-Control} header.
     */
    public CacheControl cacheControl() {
        CacheControl result = cacheControl;
        return result != null ? result : (cacheControl = CacheControl.parse(headers));
    }

    public boolean isHttps() {
        return url.isHttps();
    }

    @Override public String toString() {
        return "Request{method="
                + method
                + ", url="
                + url
                + ", tag="
                + (tag != this ? tag : null)
                + '}';
    }

    public String path() {
        return this.path;
    }

    public Context context(){
        return this.ctx;
    }

    public DBHelper dbHelper(){
        return this.db;
    }

    public boolean isFinished() {
        return this.isFinished;
    }

    public Call call() {
        return this.call;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        this.call = call;
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
    public void onResponse(Call call, Response response) throws IOException {
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
                errorMessage = ErrorString.NULL_CONTENTS;
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
                errorMessage = ErrorString.NULL_CONTENTS;
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
        final Request me = this;
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
        final Request me = this;
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

    protected void onSuccess(final Response response, final String responMessage) {
        final Request me = this;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestSuccess(me, response, responMessage);
                }
            });
        } else {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    requestHandler.onHTTPRequestSuccess(me, response, responMessage);
                }
            };
            thread.start();
        }
    }

    protected void onFail(final Throwable error) {
        final Request me = this;
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
        final Request me = this;
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
        final Request me = this;
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

    public static class Builder {
        private Context ctx                         = null;
        private HttpUrl url                         = null;
        private String path                         = null;
        private String method                       = "";
        private Headers.Builder headers             = null;
        private @Nullable RequestBody body          = null;
        private Object tag                          = null;
        private DBHelper db                         = null;
        private Map<String, Object> params          = new HashMap<String, Object>();
        private volatile CacheControl cacheControl  = null; // Lazily initialized.
        private RequestListener requestHandler      = null;
        private Call call                           = null;
        private boolean isFinished                  = false;
        private boolean containFile                 = false;
        private long contentLength                  = 0;

        public Builder() {
            super();
            this.method         = HttpMethod.GET;
            this.headers        = new Headers.Builder();
        }

        public Builder(Context ctx) {
            super();
            this.method         = HttpMethod.GET;
            this.headers        = new Headers.Builder();
            this.context(ctx);
        }

        public Builder(Request request) {
            this.ctx                = request.ctx;
            this.url                = request.url;
            this.path               = request.path;
            this.method             = request.method;
            this.headers            = request.headers.newBuilder();
            this.body               = request.body;
            this.tag                = request.tag;
            this.db                 = request.db;
            this.params             = request.params;
            this.requestHandler     = request.requestHandler;
            this.cacheControl       = request.cacheControl;
            this.call               = request.call;
        }

        public Builder context(Context ctx){
            this.ctx = ctx;
            return this;
        }

        /**
         * Sets the URL target of this request.
         *
         * @throws IllegalArgumentException if {@code url} is not a valid HTTP or HTTPS URL. Avoid this
         * exception by calling {@link HttpUrl#parse}; it returns null for invalid URLs.
         */

        public Builder url(@NotNull String url) {
            if (url == null) throw new NullPointerException("url == null");

            // Silently replace web socket URLs with HTTP URLs.
            if (url.regionMatches(true, 0, "ws:", 0, 3)) {
                url = "http:" + url.substring(3);
            } else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
                url = "https:" + url.substring(4);
            }

            HttpUrl parsed = HttpUrl.parse(url);
            if (parsed != null) {
                return this.url(parsed);
            }
            throw new IllegalArgumentException("unexpected url: " + url);
        }

        public Builder url(@NotNull HttpUrl url) {
            if (url == null) throw new NullPointerException("url == null");
            this.url = url;
            return this;
        }

        /**
         * Sets the URL target of this request.
         *
         * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code
         * https}.
         */
        public Builder url(@NotNull URL url) {
            if (url == null) throw new NullPointerException("url == null");
            HttpUrl parsed = HttpUrl.get(url);
            if (parsed == null) throw new IllegalArgumentException("unexpected url: " + url);
            return this.url(parsed);
        }

        /**
         * Sets the header named {@code name} to {@code value}. If this request already has any headers
         * with that name, they are all replaced.
         */
        public Builder header(String name, String value) {
            headers.set(name, value);
            return this;
        }

        /**
         * Adds a header with {@code name} and {@code value}. Prefer this method for multiply-valued
         * headers like "Cookie".
         *
         * <p>Note that for some headers including {@code Content-Length} and {@code Content-Encoding},
         * OkHttp may replace {@code value} with a header derived from the request body.
         */
        public Builder addHeader(String name, String value) {
            headers.add(name, value);
            return this;
        }

        public Builder addHeader(KeyValuePair pair) {
            return this.addHeader(pair.getKey(), pair.getValue());
        }

        public Builder removeHeader(String name) {
            headers.removeAll(name);
            return this;
        }

        /** Removes all headers on this builder and adds {@code headers}. */
        public Builder headers(Headers headers) {
            this.headers = headers.newBuilder();
            return this;
        }

        /**
         * Sets this request's {@code Cache-Control} header, replacing any cache control headers already
         * present. If {@code cacheControl} doesn't define any directives, this clears this request's
         * cache-control headers.
         */
        public Builder cacheControl(CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            String value = cacheControl.toString();
            if (value.isEmpty()) return removeHeader("Cache-Control");
            return header("Cache-Control", value);
        }

        public Builder get() {
            return method(HttpMethod.GET, null);
        }

        public Builder head() {
            return method(HttpMethod.HEAD, null);
        }

        public Builder post(RequestBody body) {
            return method(HttpMethod.POST, body);
        }

        public Builder delete(@Nullable RequestBody body) {
            return method(HttpMethod.DELETE, body);
        }

        public Builder delete() {
            return delete(Util.EMPTY_REQUEST);
        }

        public Builder put(RequestBody body) {
            return method(HttpMethod.PUT, body);
        }

        public Builder patch(RequestBody body) {
            return method(HttpMethod.PATCH, body);
        }

        public Builder method(String method, @Nullable RequestBody body) {
            if (method == null) throw new NullPointerException("method == null");
            if (method.length() == 0) throw new IllegalArgumentException("method.length() == 0");
            if (body != null && !okhttp3.internal.http.HttpMethod.permitsRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must not have a request body.");
            }
            if (body == null && okhttp3.internal.http.HttpMethod.requiresRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must have a request body.");
            }
            this.method = method;
            this.body = body;
            return this;
        }

        /**
         * Attaches {@code tag} to the request. It can be used later to cancel the request. If the tag
         * is unspecified or null, the request is canceled by using the request itself as the tag.
         */
        public Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Request build() {
            return this.build(false);
        }

        public Request build(boolean strict) {
            this.url = this.buildURL(strict);
            this.body = this.buildBody(strict);
            if (!this.hasContentTypeHeader()) this.addContentLengthHeader().addContentTypeHeader();
            return new Request(this);
        }

        // Additional Methods

        public Builder path(@NotNull String path){
            if (path == null) throw new NullPointerException("baseURL == null");
            path = path.startsWith("/") ? path.substring(1) : path;
            this.path = path;
            return this;
        }

        public Builder path(@NotNull String path, Object... arguments){
            if (path == null) throw new NullPointerException("baseURL == null");
            path = path.startsWith("/") ? path.substring(1) : path;
            this.path = String.format(path, arguments);
            return this;
        }

        public Builder method(String method){
            this.method = method;
            return this;
        }

        public Builder params(Map<String, Object> params){
            this.params = new HashMap<>();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                this.addParam(param.getKey(), param.getValue());
            }
            return this;
        }

        public Builder addParam(String key, Object value) {
            this.params.put(key, value);
            if (value instanceof File) {
                this.containFile = true;
            }
            return this;
        }

        public Builder authBasicHeader(String login, String password) {
            return this.addHeader("Authorization", Credentials.basic(login, password));
        }

        public Builder authHeader(String auth, String token) {
            return this.addHeader("Authorization", auth + " " + token);
        }

        public Builder listener(RequestListener requestHandler){
            this.requestHandler = requestHandler;
            return this;
        }

        public Builder body(RequestBody body) {
            this.body = body;
            return this;
        }

        public Builder dbHelper(DBHelper db) {
            this.db = db;
            return this;
        }

        public Builder addContentTypeHeader() {
            return this.addHeader("Content-Type",    this.body.contentType().toString());
        }

        public Builder addContentLengthHeader() {
            return this.addHeader("Content-Length",  Long.toString(this.getContentLength()));
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

        private HttpUrl buildURL(boolean strict) {
            if (strict) {
                if (this.url == null) throw new NullPointerException("url == null");
            } else {
                if (this.url == null) return null;
            }
            if (this.url == null) throw new NullPointerException("url == null");
            HttpUrl.Builder builder = this.url.newBuilder();
            if (this.path != null) {
                builder.addPathSegment(this.path);
            }
            if (this.method.equals(HttpMethod.GET)) {
                for (Map.Entry<String, Object> param : this.params.entrySet()) {
                    String param_value = "";
                    if (param.getValue() != null) {
                        param_value = param.getValue().toString();
                    }
                    builder.addQueryParameter(param.getKey(), param_value);
                }
            }
            return builder.build();
        }

        private RequestBody buildBody(boolean strict) {
            if (!okhttp3.internal.http.HttpMethod.permitsRequestBody(this.method)) {
                return null;
            }
            if (this.body != null && this.params.size() == 0) {
                return this.body;
            }
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
            return this.body;
        }
    }
}
