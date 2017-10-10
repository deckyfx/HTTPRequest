# httprequest
[![](https://jitpack.io/v/deckyfx/httprequest.svg)](https://jitpack.io/#httprequest/dbsession)

Extension of okhttp3, with many short hand methods, and event listeners 

* okhttp3
* deckyfx/dbhelper
* deckyfx/simpleadapter
* greendao

## Sample CodUsage

Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
	repositories {
	...
		maven { url 'https://jitpack.io' }
	}
}
```
Add the dependency

```gradle
dependencies {
    compile 'com.github.deckyfx.httprequest:-SNAPSHOT'
    compile group: 'com.squareup.okhttp3', name: 'okhttp', version: '3.9.0'
}
```



## Sample Code


Init the class and configure it
```java
...
HTTPRequest HTTP_CLIENT = new HTTPRequest(App.MAIN_CONTEXT);
HTTP_CLIENT.setRequestTimeOut(15);
HTTP_CLIENT.initRequestCache();
HTTP_CLIENT.initCookieStore();
HTTP_CLIENT.setBaseURL(/* your REST API home url */);
HTTP_CLIENT.addApplicationInterceptor(/* Interceptor goes here */);
HTTP_CLIENT.enableHTTPLogging(/* HttpLoggingInterceptor.Level */);
HTTP_CLIENT.enableChuckLogging(/* Should we show notification? */);
HTTP_CLIENT.initHTTPCLient();
...

```

Then Start Request by Calling
```java
...
Request request = new Request.Builder(this)
            .url("http://google.com")
            .build();
this.G.HTTP_CLIENT.send(request);
...

```

POST data
```java
...
Request request = new Request.Builder(this)
            .path("/apipath")
            .addParam("name", "John Doe")
            .method(HttpMethod.POST)
            .build();
this.G.HTTP_CLIENT.send(request);
...
```

Upload File
```java
...
Request request = new Request.Builder(this)
           .path("/apipath")
           .addParam("file", new File(/* File Path*/))
           .method(HttpMethod.POST)
           .build();
this.G.HTTP_CLIENT.send(request);
...

```

Header and Listener
```java
...
Request request = new Request.Builder(this)
           .path("/apipath")
           .addHeader("XHTTPHeader", "value")
           .addParam("asGET", "value")
           .method(HttpMethod.GET)
           .listener(this)
           .build();
this.G.HTTP_CLIENT.send(request);
...

```

More sample is [here]

## Feature:

 * 

