package com.WS.tools;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class JSInterface {
    private final Context context;

    public JSInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void processDataUrl(String dataUrl, String fileName) {
        FileDownloadManager.saveDataUrlToFile(context, dataUrl, fileName);
    }
}