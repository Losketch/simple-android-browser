package com.WS.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserWebViewClient extends WebViewClient {
    private final Activity activity;
    
    public BrowserWebViewClient(Activity activity) {
        this.activity = activity;
    }
    
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        view.loadUrl("file:///android_asset/error.html");
    }
    
    @SuppressLint("WebViewClientOnReceivedSslError")
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        // 生产环境应提示用户SSL错误并给予选择
        new AlertDialog.Builder(activity)
                .setTitle("SSL证书错误")
                .setMessage("网站的安全证书存在问题，继续访问可能存在风险。是否继续？")
                .setPositiveButton("继续", (dialog, which) -> handler.proceed())
                .setNegativeButton("取消", (dialog, which) -> handler.cancel())
                .show();
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        // Ask user about resubmitting the form
        new AlertDialog.Builder(activity)
                .setTitle("警告")
                .setMessage("是否重新提交表单？")
                .setPositiveButton("确定", (dialog, which) -> resend.sendToTarget())
                .setNegativeButton("取消", (dialog, which) -> dontResend.sendToTarget())
                .show();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // 检查URL是否以http或https开头
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // 返回false表示WebView应该处理这个URL
            return false;
        } else {
            // 处理自定义URL方案的逻辑
            try {
                // 自定义处理代码，比如打开其他应用等
                return true; // 已处理
            } catch (Exception e) {
                return false; // 让WebView尝试处理
            }
        }
    }

    // 新版本的shouldOverrideUrlLoading方法
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String url = request.getUrl().toString();
            return shouldOverrideUrlLoading(view, url);
        }
        return false; // 对于旧版本，返回false，WebView处理URL
    }
}