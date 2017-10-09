package com.github.deckyfx.httprequest;

/**
 * Created by decky on 10/9/17.
 */

public class KeyValuePair {
    private String key;
    private String value;

    public KeyValuePair(String key, String value){
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public KeyValuePair key(String key) {
        this.key = key;
        return this;
    }

    public KeyValuePair value(String value) {
        this.value = value;
        return this;
    }
}
