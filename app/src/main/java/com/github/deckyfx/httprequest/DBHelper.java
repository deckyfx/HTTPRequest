package com.github.deckyfx.httprequest;

import android.content.Context;

import com.github.deckyfx.greendao.AbstractDaoMaster;
import com.github.deckyfx.greendao.Property;
import com.github.deckyfx.httprequest.dao.RequestCache;

import java.util.Date;
import java.util.List;

/**
 * Created by decky on 12/29/16.
 */
public class DBHelper extends com.github.deckyfx.dbhelper.DBHelper{
    private static final class REQUEST_CACHE {
        public static final String DAO_NAME             = "RequestCache";
        public static final String PROPERTY_URL         = "Url";
        public static final String PROPERTY_METHOD      = "Method";
        public static final String PROPERTY_PARAM       = "Param";
        public static final String PROPERTY_HEADER      = "Header";
        public static final String PROPERTY_ID          = "Id";
        public static final String PROPERTY_Response    = "Response";
    }

    public Property RequestCacheURLProperty,
            RequestCacheMethodProperty,
            RequestCacheParamProperty,
            RequestCacheHeaderProperty,
            RequestCacheIDProperty;

    public DBHelper(Context context, Class<? extends AbstractDaoMaster> daoMasterClass, String dbName) {
        super(context, daoMasterClass, dbName);


        this.RequestCacheURLProperty        = this.getEntity(REQUEST_CACHE.DAO_NAME).getProperty(REQUEST_CACHE.PROPERTY_URL);
        this.RequestCacheMethodProperty     = this.getEntity(REQUEST_CACHE.DAO_NAME).getProperty(REQUEST_CACHE.PROPERTY_METHOD);
        this.RequestCacheParamProperty      = this.getEntity(REQUEST_CACHE.DAO_NAME).getProperty(REQUEST_CACHE.PROPERTY_PARAM);
        this.RequestCacheHeaderProperty     = this.getEntity(REQUEST_CACHE.DAO_NAME).getProperty(REQUEST_CACHE.PROPERTY_HEADER);
        this.RequestCacheIDProperty         = this.getEntity(REQUEST_CACHE.DAO_NAME).getProperty(REQUEST_CACHE.PROPERTY_ID);
    }

    public void saveResponseToCache(String url, String method, String param, String responseBody){
        RequestCache requestcache = this.getResponseFromCache(url, method, param);
        Date date = new Date();
        if ( requestcache == null ) {
            requestcache = new RequestCache();
            requestcache.setCreatedAt(date);
            requestcache.setUrl(url);
            requestcache.setMethod(method);
            requestcache.setParam(param);
        }
        requestcache.setUpdatedAt(date);
        requestcache.setResponse(responseBody);
        this.getEntity(REQUEST_CACHE.DAO_NAME).insertOrReplace(requestcache);
    }

    public String loadResponseFromCache(String url, String method, String param){
        RequestCache requestcache = this.getResponseFromCache(url, method, param);
        byte[] response = null;
        if ( requestcache != null ) {
            return requestcache.getResponse();
        }
        return "";
    }

    public RequestCache getResponseFromCache(String url, String method, String param){
        List requestCacheList = this.getEntity(REQUEST_CACHE.DAO_NAME)
                .queryBuilder()
                .where(this.RequestCacheURLProperty.eq(url), this.RequestCacheMethodProperty.eq(method), this.RequestCacheParamProperty.eq(param)).limit(1)
                .orderAsc(this.RequestCacheIDProperty).list();
        RequestCache requestcache = null;
        byte[] response = null;
        if (requestCacheList.size() > 0) {
            requestcache = (RequestCache)requestCacheList.get(0);
        }
        return requestcache;
    }
}
