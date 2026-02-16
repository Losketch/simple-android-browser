package com.WS.tools;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class WebViewManager {
    private final Activity activity;
    private final WebView webView;
    private ValueCallbackListener filePathCallbackListener;
    private ChildWebViewListener childWebViewListener;

    public interface ValueCallbackListener {
        void onValueCallback(ValueCallback<Uri[]> callback);
    }

    public interface ChildWebViewListener {
        void onChildWebView(WebView webView);
    }

    public WebViewManager(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    public void setFilePathCallbackListener(ValueCallbackListener listener) {
        this.filePathCallbackListener = listener;
    }

    public void setChildWebViewListener(ChildWebViewListener listener) {
        this.childWebViewListener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        configureWebSettings(webSettings);
        setupCookieManager();
        setupWebViewClient();
        setupWebChromeClient();
        setupDownloadListener();
        setupJavaScriptInterface();
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void configureWebSettings(WebSettings webSettings) {
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
    }

    private void setupCookieManager() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
    }

    private void setupWebViewClient() {
        webView.setWebViewClient(new BrowserWebViewClient(activity));
    }

    private void setupWebChromeClient() {
        ProgressBar progressBar = activity.findViewById(R.id.progressBar);
        BrowserChromeClient chromeClient = new BrowserChromeClient(
                activity,
                progressBar,
                webView,
                callback -> {
                    if (filePathCallbackListener != null) {
                        filePathCallbackListener.onValueCallback(callback);
                    }
                },
                childWebView -> {
                    if (childWebViewListener != null) {
                        childWebViewListener.onChildWebView(childWebView);
                    }
                }
        );
        webView.setWebChromeClient(chromeClient);
    }
    
    private void setupDownloadListener() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (url.startsWith("blob:")) {
                handleBlobDownload(url, contentDisposition, mimeType);
            } else {
                FileDownloadManager.downloadFile(activity, url, userAgent, contentDisposition, mimeType);
            }
        });
    }

    private void handleBlobDownload(String url, String contentDisposition, String mimeType) {
        String fileName = extractFileNameFromContentDisposition(contentDisposition);
        if (fileName == null) {
            fileName = "download";
        }
        
        String jsCode = "(async function() {" +
                "try {" +
                    "const response = await fetch('" + url + "');" +
                    "const blob = await response.blob();" +
                    "const reader = new FileReader();" +
                    "reader.onload = function() {" +
                        "window.HTMLOUT.processDataUrl(reader.result, '" + fileName + "');" +
                    "};" +
                    "reader.readAsDataURL(blob);" +
                "} catch(e) {" +
                    "window.HTMLOUT.onBlobDownloadError(e.message);" +
                "}" +
            "})();";

        webView.evaluateJavascript(jsCode, null);
    }

    private String extractFileNameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return null;
        }

        // Handle filename*=UTF-8''filename.txt format
        int utf8Index = contentDisposition.indexOf("filename*=UTF-8''");
        if (utf8Index != -1) {
            String filename = contentDisposition.substring(utf8Index + "filename*=UTF-8''".length());
            // Remove possible trailing semicolon
            int semicolonIndex = filename.indexOf(";");
            if (semicolonIndex != -1) {
                filename = filename.substring(0, semicolonIndex);
            }
            return filename.trim();
        }

        // Handle filename="filename.txt" format
        int filenameIndex = contentDisposition.indexOf("filename=");
        if (filenameIndex != -1) {
            String filenamePart = contentDisposition.substring(filenameIndex + "filename=".length());
            if (filenamePart.startsWith("\"")) {
                // Find closing quote (starting from second character)
                int endQuoteIndex = filenamePart.indexOf("\"", 1);
                if (endQuoteIndex != -1) {
                    return filenamePart.substring(1, endQuoteIndex).trim();
                }
            } else {
                int endIndex = filenamePart.indexOf(";");
                if (endIndex != -1) {
                    return filenamePart.substring(0, endIndex).trim();
                }
                return filenamePart.trim();
            }
        }

        return null;
    }

    private void setupJavaScriptInterface() {
        webView.addJavascriptInterface(new JSInterface(activity), "HTMLOUT");
    }
}