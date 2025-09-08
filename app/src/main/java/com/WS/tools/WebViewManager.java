package com.WS.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
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

    private void configureWebSettings(WebSettings webSettings) {
        // Basic settings
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Advanced settings
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

        // Version-specific settings
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
        // 获取并修正文件名
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        fileName = FileDownloadManager.ensureCorrectExtension(fileName, mimeType);

        final String finalFileName = fileName;

        webView.evaluateJavascript(
                "(function() {" +
                        "    var xhr = new XMLHttpRequest();" +
                        "    xhr.open('GET', '" + url + "', true);" +
                        "    xhr.responseType = 'blob';" +
                        "    xhr.onload = function(e) {" +
                        "        if (this.status == 200) {" +
                        "            var blob = this.response;" +
                        "            var reader = new FileReader();" +
                        "            reader.readAsDataURL(blob);" +
                        "            reader.onloadend = function() {" +
                        "                window.HTMLOUT.processDataUrl(reader.result, '" + finalFileName + "');" +
                        "            }" +
                        "        }" +
                        "    };" +
                        "    xhr.send();" +
                        "})();",
                null
        );
    }

    private void setupJavaScriptInterface() {
        webView.addJavascriptInterface(new JSInterface(activity), "HTMLOUT");
    }
}