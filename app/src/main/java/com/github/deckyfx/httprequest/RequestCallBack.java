package com.github.deckyfx.httprequest;

import android.app.Activity;
import android.content.Context;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by decky on 2/2/17.
 */
public class RequestCallBack implements Callback {
    private final Map<String, Object> params, headers;
    private DBHelper db;
    private Context ctx;
    private RequestHandler requestHandler;
    private Call mCall;
    private RequestCallBack me;
    private boolean isFinished;

    public RequestCallBack(Context ctx,
                           Call call,
                           Map<String, Object> params,
                           Map<String, Object> headers,
                           RequestHandler requestHandler,
                           DBHelper db) {
        this.ctx                = ctx;
        this.db                 = db;
        this.mCall              = call;
        this.params             = params;
        this.headers            = headers;
        this.requestHandler     = requestHandler;
        this.me                 = this;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        this.mCall = call;
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
    public void onResponse(Call call, Response response) throws IOException {
        this.mCall = call;
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
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestStart(me);
                }
            });
        } else {
            requestHandler.onHTTPRequestStart(me);
        }
    }

    protected void onFinish() {
        this.isFinished = true;
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestFinish(me);
                }
            });
        } else {
            requestHandler.onHTTPRequestFinish(me);
        }
    }

    protected void onSuccess(final Response response, final String responMessage) {
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestSuccess(me, response, responMessage);
                }
            });
        } else {
            requestHandler.onHTTPRequestSuccess(me, response, responMessage);
        }
    }

    protected void onFail(final Throwable error) {
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestFailure(me, error);
                }
            });
        } else {
            requestHandler.onHTTPRequestFailure(me, error);
        }
    }

    protected void onRescue(final String recoveredResponse) {
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestRescue(me, recoveredResponse);
                }
            });
        } else {
            requestHandler.onHTTPRequestRescue(me, recoveredResponse);
        }
    }

    protected void onNetworkError() {
        if (this.ctx instanceof Activity) {
            ((Activity) this.ctx).runOnUiThread(new Runnable() {
                public void run() {
                    requestHandler.onHTTPRequestNetworkError(me);
                }
            });
        }
    }

    public Call getCall() {
        return this.mCall;
    }

    public Request getRequest() {
        return this.mCall.request();
    }

    public boolean isFinished() {
        return this.isFinished;
    }
}