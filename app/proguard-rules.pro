# WebView rules - CRITICAL for browser functionality
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# Keep WebView JavaScript interface
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep all WebView related classes
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# Network security
-keep class android.security.NetworkSecurityPolicy { *; }
-keep class android.net.http.** { *; }

# Keep application class
-keep class com.naxobrowser.** { *; }
