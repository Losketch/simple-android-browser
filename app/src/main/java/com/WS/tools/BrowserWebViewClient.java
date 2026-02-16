package com.WS.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserWebViewClient extends WebViewClient {
    private final Activity activity;
    private final boolean isDarkMode;
    
    public BrowserWebViewClient(Activity activity) {
        this.activity = activity;
        int nightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        this.isDarkMode = (nightMode == Configuration.UI_MODE_NIGHT_YES);
    }
    
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        view.loadUrl("file:///android_asset/error.html");
    }
    
    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        injectMinimalDarkModeScript(view);
    }
    
    private void injectMinimalDarkModeScript(WebView view) {
        String script = "(function() {" +
                "if (window.__androidDarkModeInjected) return;" +
                "window.__androidDarkModeInjected = true;" +
                "if (window.matchMedia) {" +
                "var orig = window.matchMedia;" +
                "window.matchMedia = function(q) {" +
                "var mql = orig(q);" +
                "if (q === '(prefers-color-scheme: dark)') {" +
                "Object.defineProperty(mql, 'matches', { value: " + isDarkMode + ", writable: false });" +
                "}" +
                "if (q === '(prefers-color-scheme: light)') {" +
                "Object.defineProperty(mql, 'matches', { value: " + !isDarkMode + ", writable: false });" +
                "}" +
                "return mql;" +
                "};" +
                "}" +
                "})();";
        
        view.evaluateJavascript(script, null);
    }
    
    @SuppressLint("WebViewClientOnReceivedSslError")
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        new AlertDialog.Builder(activity)
                .setTitle("SSL证书错误")
                .setMessage("网站的安全证书存在问题，继续访问可能存在风险。是否继续？")
                .setPositiveButton("继续", (dialog, which) -> handler.proceed())
                .setNegativeButton("取消", (dialog, which) -> handler.cancel())
                .show();
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        new AlertDialog.Builder(activity)
                .setTitle("警告")
                .setMessage("是否重新提交表单？")
                .setPositiveButton("确定", (dialog, which) -> resend.sendToTarget())
                .setNegativeButton("取消", (dialog, which) -> dontResend.sendToTarget())
                .show();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false;
        } else {
            try {
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String url = request.getUrl().toString();
            return shouldOverrideUrlLoading(view, url);
        }
        return false;
    }
}