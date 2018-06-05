package com.github.deckyfx.httprequest;

/**
 * Created by decky on 10/9/17.
 */

public class KeyValuePair {
    private String key;
    private Object value;

    public KeyValuePair(String key, Object value){
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public String getValueAsString() {
        return String.valueOf(value);
    }

    public KeyValuePair key(String key) {
        this.key = key;
        return this;
    }

    public KeyValuePair value(Object value) {
        this.value = value;
        return this;
    }
}
