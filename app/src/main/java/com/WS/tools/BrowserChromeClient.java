package com.WS.tools;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;

public class BrowserChromeClient extends WebChromeClient {
    private final Activity activity;
    private final ProgressBar progressBar;
    private final WebView mainWebView;
    private final ValueCallbackListener filePathCallbackListener;
    private final ChildWebViewListener childWebViewListener;
    
    public interface ValueCallbackListener {
        void onValueCallback(ValueCallback<Uri[]> callback);
    }
    
    public interface ChildWebViewListener {
        void onChildWebView(WebView webView);
    }
    
    public BrowserChromeClient(Activity activity, ProgressBar progressBar, WebView mainWebView,
                               ValueCallbackListener filePathCallbackListener,
                               ChildWebViewListener childWebViewListener) {
        this.activity = activity;
        this.progressBar = progressBar;
        this.mainWebView = mainWebView;
        this.filePathCallbackListener = filePathCallbackListener;
        this.childWebViewListener = childWebViewListener;
    }
    
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePath,
                                   FileChooserParams fileChooserParams) {
        if (filePathCallbackListener != null) {
            filePathCallbackListener.onValueCallback(filePath);
        }

        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            activity.startActivityForResult(Intent.createChooser(intent, "选择文件"), 
                    1); // FILE_CHOOSER_RESULT_CODE
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(activity)
            .setTitle("提示")
            .setMessage(message)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            })
            .setCancelable(false)
            .create()
            .show();
        return true;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(activity)
            .setTitle("确认")
            .setMessage(message)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            })
            .setCancelable(false)
            .create()
            .show();
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        new AlertDialog.Builder(activity)
                .setTitle("位置权限")
                .setMessage(origin + " 请求获取位置信息")
                .setPositiveButton("允许", (dialog, which) -> callback.invoke(origin, true, false))
                .setNegativeButton("拒绝", (dialog, which) -> callback.invoke(origin, false, false))
                .show();
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(newProgress);
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        WebView newWebView = new WebView(activity);
        setupNewWebView(newWebView);

        FrameLayout webContainer = activity.findViewById(R.id.web_container);
        webContainer.addView(newWebView);
        webContainer.setVisibility(View.VISIBLE);

        mainWebView.setVisibility(View.GONE);

        if (childWebViewListener != null) {
            childWebViewListener.onChildWebView(newWebView);
        }

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();

        return true;
    }

    private void setupNewWebView(WebView newWebView) {
        WebSettings newSettings = newWebView.getSettings();
        WebSettings mainSettings = mainWebView.getSettings();

        // Basic settings
        newSettings.setJavaScriptEnabled(mainSettings.getJavaScriptEnabled());
        newSettings.setDomStorageEnabled(mainSettings.getDomStorageEnabled());
        newSettings.setDatabaseEnabled(mainSettings.getDatabaseEnabled());
        newSettings.setMixedContentMode(mainSettings.getMixedContentMode());

        // Security and storage
        newSettings.setAllowFileAccess(mainSettings.getAllowFileAccess());
        newSettings.setAllowContentAccess(mainSettings.getAllowContentAccess());

        // Set WebViewClient
        newWebView.setWebViewClient(new BrowserWebViewClient(activity));
        newWebView.setWebChromeClient(this);
    }

    @Override
    public void onCloseWindow(WebView window) {
        super.onCloseWindow(window);
    }
}