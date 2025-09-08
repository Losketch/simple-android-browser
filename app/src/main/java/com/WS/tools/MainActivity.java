package com.WS.tools;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

public class MainActivity extends Activity {
    protected ValueCallback<Uri[]> filePathCallback;
    private WebView mWebView;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;
    private WebView currentChildWebView = null;
    private PermissionsHandler permissionsHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        
        // Initialize WebView and managers
        mWebView = findViewById(R.id.activity_main_webview);
        registerForContextMenu(mWebView);
        
        // Initialize handlers
        permissionsHandler = new PermissionsHandler(this);
        permissionsHandler.checkAndRequestStoragePermission();
        
        // Initialize WebView manager
        WebViewManager webViewManager = new WebViewManager(this, mWebView);
        webViewManager.setupWebView();
        webViewManager.setFilePathCallbackListener(callback -> filePathCallback = (ValueCallback<Uri[]>) callback);
        webViewManager.setChildWebViewListener(webView -> currentChildWebView = webView);
        
        // Start local server and load URL
        LocalAssetServer.start(this);
        mWebView.loadUrl(LocalAssetServer.getLocalUrl());
    }

    @Override
    protected void onDestroy() {
        LocalAssetServer.stop();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (filePathCallback == null) return;
            
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (currentChildWebView != null) {
            if (currentChildWebView.canGoBack()) {
                currentChildWebView.goBack();
            } else {
                closeChildWindow(currentChildWebView);
            }
        } else if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void closeChildWindow(WebView childWebView) {
        if (childWebView == null) return;

        FrameLayout webContainer = findViewById(R.id.web_container);
        webContainer.removeView(childWebView);
        webContainer.setVisibility(View.GONE);

        childWebView.destroy();
        mWebView.setVisibility(View.VISIBLE);

        if (currentChildWebView == childWebView) {
            currentChildWebView = null;
        }
    }

    public void onForwardPressed(View view) {
        if (mWebView.canGoForward()) {
            mWebView.goForward();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView.HitTestResult result = mWebView.getHitTestResult();
        if (result.getType() == WebView.HitTestResult.IMAGE_TYPE) {
            menu.add(0, 1, 0, "保存图片");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        WebView.HitTestResult result = mWebView.getHitTestResult();
        if (item.getItemId() == 1 && result.getType() == WebView.HitTestResult.IMAGE_TYPE) {
            String imageUrl = result.getExtra();
            FileDownloadManager.downloadImage(this, imageUrl);
            return true;
        }
        return super.onContextItemSelected(item);
    }
}