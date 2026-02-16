package com.WS.tools;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class JSInterface {
    private final Context context;

    public JSInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void downloadBlob(String dataUrl, String fileName) {
        try {
            // Directly process the data URL with the provided filename
            FileDownloadManager.saveDataUrlToFile(context, dataUrl, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(context, "Download failed: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }
    
    @JavascriptInterface
    public void processDataUrl(String dataUrl, String fileName) {
        FileDownloadManager.saveDataUrlToFile(context, dataUrl, fileName);
    }

    @JavascriptInterface
    public void processBlobDataNative(Integer[] byteArray, String fileName, String mimeType) {
        try {
            if (byteArray == null || byteArray.length == 0) {
                android.widget.Toast.makeText(context, "下载失败: 获取到的文件数据为空", android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            byte[] decodedData = new byte[byteArray.length];
            for (int i = 0; i < byteArray.length; i++) {
                if (byteArray[i] == null) {
                    android.widget.Toast.makeText(context, "下载失败: 文件数据不完整", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                decodedData[i] = byteArray[i].byteValue();
            }
            FileDownloadManager.saveBlobDataToFile(context, decodedData, fileName, mimeType);
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(context, "下载失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void onBlobDownloadError(String errorMessage) {
        android.widget.Toast.makeText(context, "Blob下载失败: " + errorMessage, android.widget.Toast.LENGTH_LONG).show();
    }
}