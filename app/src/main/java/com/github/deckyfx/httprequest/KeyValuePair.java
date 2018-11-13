package com.github.deckyfx.httprequest;

/**
 * Created by decky on 10/9/17.
 */

public class KeyValuePair {
    private String key;
    private Object value;
    private boolean encode;

    public KeyValuePair(String key, Object value){
        this(key, value, true);
    }

    public KeyValuePair(String key, Object value, boolean encode){
        this.key            = key;
        this.value          = value;
        this.encode         = encode;
    }

    public String getKey() {
        return this.key;
    }

    public Object getValue() {
        return this.value;
    }

    public boolean shouldEncode() {
        return this.encode;
    }

    public String getValueAsString() {
        return String.valueOf(this.value);
    }

    public KeyValuePair key(String key) {
        this.key = key;
        return this;
    }

    public KeyValuePair value(Object value) {
        this.value = value;
        return this;
    }

    public KeyValuePair encode(boolean value) {
        this.encode = value;
        return this;
    }
}
