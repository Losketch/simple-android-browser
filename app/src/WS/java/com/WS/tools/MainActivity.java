package com.WS.tools;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.GeolocationPermissions;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {
    protected ValueCallback<Uri[]> filePathCallback;
    private WebView mWebView;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;

    Intent intent = VpnService.prepare(this);
    if (intent != null) {
        startActivityForResult(intent, VPN_REQUEST_CODE);
    } else {
        startService(new Intent(this, CustomVpnService.class));
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();
        registerForContextMenu(mWebView);

        // 增强的 WebView 设置
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        }
        // 新增的浏览器功能设置
        webSettings.setBuiltInZoomControls(true);  // 启用缩放
        webSettings.setDisplayZoomControls(false);  // 隐藏原生缩放按钮
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);  // 页面自适应屏幕
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);  // 支持多窗口
        webSettings.setGeolocationEnabled(true);  // 启用地理位置
        webSettings.setMediaPlaybackRequiresUserGesture(false);  // 允许自动播放媒体


        // 设置 WebViewClient
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // 显示自定义错误页面
                view.loadUrl("file:///android_asset/error.html");
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // 允许继续加载（仅限测试环境！）
                handler.proceed();
            }
            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                // 弹窗询问用户是否重新提交
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("警告")
                        .setMessage("是否重新提交表单？")
                        .setPositiveButton("确定", (dialog, which) -> resend.sendToTarget())
                        .setNegativeButton("取消", (dialog, which) -> dontResend.sendToTarget())
                        .show();
            }
        });//*/

        // 设置 WebChromeClient
        mWebView.setWebChromeClient(new WebChromeClient() {
            // 处理文件选择
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePath,
                                           FileChooserParams fileChooserParams) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = filePath;

                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), 
                            FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            // 处理 JavaScript alert 弹窗
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
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

            // 处理 JavaScript confirm 确认框
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
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
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("位置权限")
                        .setMessage(origin + " 请求获取位置信息")
                        .setPositiveButton("允许", (dialog, which) -> callback.invoke(origin, true, false))
                        .setNegativeButton("拒绝", (dialog, which) -> callback.invoke(origin, false, false))
                        .show();
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @TargetApi(Build.VERSION_CODES.O)
            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    Message resultMsg
            ) {
                // 创建新的 WebView 实例
                WebView newWebView = new WebView(MainActivity.this);

                // 复制主 WebView 的所有配置
                WebSettings newSettings = newWebView.getSettings();
                WebSettings mainSettings = mWebView.getSettings();

                // 基础设置
                newSettings.setJavaScriptEnabled(mainSettings.getJavaScriptEnabled());
                newSettings.setDomStorageEnabled(mainSettings.getDomStorageEnabled());
                newSettings.setDatabaseEnabled(mainSettings.getDatabaseEnabled());
                newSettings.setMixedContentMode(mainSettings.getMixedContentMode());

                // 安全与存储
                newSettings.setAllowFileAccess(mainSettings.getAllowFileAccess());
                newSettings.setAllowContentAccess(mainSettings.getAllowContentAccess());

                // 绑定 WebViewClient 和 WebChromeClient
                newWebView.setWebViewClient(new WebViewClient());
                newWebView.setWebChromeClient(mWebView.getWebChromeClient());

                // 添加新 WebView 到布局容器
                FrameLayout webContainer = findViewById(R.id.web_container);
                webContainer.addView(newWebView);

                // 绑定到新窗口
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();

                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                FrameLayout webContainer = findViewById(R.id.web_container);
                webContainer.removeView(window); // 从布局中移除
                window.destroy(); // 销毁 WebView
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            // 使用系统下载管理器
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "开始下载...", Toast.LENGTH_SHORT).show();
            }
        });


        mWebView.loadUrl("https://losketch.github.io/fun-html/dist/index.html");
    }

    // 处理文件选择结果
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // 权限被拒绝，提示用户
                Toast.makeText(this, "存储权限被拒绝，文件功能可能无法使用！", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
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
            // 实现图片下载逻辑
            return true;
        }
        return super.onContextItemSelected(item);
    }
}

public class CustomVpnService extends VpnService {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Builder builder = new Builder();
        builder.addAddress("10.0.0.2", 24) // 虚拟 IP
                .addDnsServer("1.1.1.1") // 自定义 DNS
                .setSession("Custom DNS VPN")
                .setConfigureIntent(null);
        ParcelFileDescriptor vpnInterface = builder.establish();
        return START_STICKY;
    }
}