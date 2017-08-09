# httprequest
[![](https://jitpack.io/v/deckyfx/httprequest.svg)](https://jitpack.io/#httprequest/dbsession)

Android user session stored in sqlite db, this is implementation of 

* deckyfx/dbhelper
* deckyfx/simpleadapter
* gson
* greendao

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
    compile 'com.google.code.gson:gson:2.8.1'
    compile 'org.greenrobot:greendao:3.2.0'
    compile 'com.github.deckyfx:simpleadapter:0.22@aar'
    compile 'com.github.deckyfx:dbhelper:0.5'
    compile 'com.github.deckyfx:httprequest:0.18'
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
HTTP_CLIENT.initHTTPCLient();
...

```

Then init DBSession
```java
...
HashMap<String, Object> params = new HashMap<String, Object>();
HashMap<String, Object> headers = new HashMap<String, Object>();
this.G.HTTP_CLIENT.send(/* context */, /* path or full url */, /* Method */, params, headers, /* request ID */, /* callback */);
...

```

More sample is [here]

## Feature:

 * 

