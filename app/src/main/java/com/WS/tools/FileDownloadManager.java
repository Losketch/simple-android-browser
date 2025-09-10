package com.WS.tools;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileDownloadManager {

    public static void downloadFile(Context context, String url, String userAgent,
                                    String contentDisposition, String mimeType) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimeType);
        request.addRequestHeader("User-Agent", userAgent);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // 获取推测的文件名并确保它有正确的扩展名
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        fileName = ensureCorrectExtension(fileName, mimeType);

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            dm.enqueue(request);
            Toast.makeText(context, "开始下载...", Toast.LENGTH_SHORT).show();
        }
    }

    public static void downloadImage(Context context, String imageUrl) {
        // Implement image download logic here
        Toast.makeText(context, "正在下载图片...", Toast.LENGTH_SHORT).show();

        // Can use the downloadFile method or implement specific image download logic
        downloadFile(context, imageUrl, "WebView",
                     "attachment", getMimeType(imageUrl));
    }

    private static final String TAG = "FileDownloadManager";

    public static void saveDataUrlToFile(Context context, String dataUrl, String fileName) {
        try {
            if (dataUrl != null && dataUrl.startsWith("data:")) {
                // 从data URL中提取MIME类型
                String mimeType = "application/octet-stream";
                if (dataUrl.contains(";")) {
                    mimeType = dataUrl.substring(5, dataUrl.indexOf(";"));
                }

                // 确保文件名具有正确的扩展名
                fileName = ensureCorrectExtension(fileName, mimeType);

                String base64Data = dataUrl.substring(dataUrl.indexOf(",") + 1);
                byte[] decodedData = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveFileUsingMediaStore(context, decodedData, fileName, mimeType);
                } else {
                    saveFileDirectly(context, decodedData, fileName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "下载失败", e);
            Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void saveFileUsingMediaStore(Context context, byte[] data, String fileName, String mimeType) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }
        if (uri != null) {
            OutputStream os = resolver.openOutputStream(uri);
            if (os != null) {
                os.write(data);
                os.close();
                Toast.makeText(context, "文件已保存到下载目录: " + fileName, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static void saveFileDirectly(Context context, byte[] data, String fileName) throws Exception {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // 检查目录是否存在，如果不存在则创建，并检查创建结果
        if (!downloadsDir.exists()) {
            if (!downloadsDir.mkdirs()) {
                Log.e(TAG, "无法创建下载目录: " + downloadsDir.getAbsolutePath());
                throw new Exception("无法创建下载目录");
            }
        }

        File file = new File(downloadsDir, fileName);

        // 使用 try-with-resources 确保流被正确关闭
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush(); // 确保数据写入
            Log.i(TAG, "文件下载成功: " + file.getAbsolutePath());
            Toast.makeText(context, "文件已下载到" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "写入文件失败: " + file.getAbsolutePath(), e);
            throw new Exception("文件写入失败: " + e.getMessage());
        }
    }

    public static String getMimeType(String fileUrl) {
        // 如果URL包含查询参数，先移除
        String cleanUrl = fileUrl;
        if (cleanUrl.contains("?")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.indexOf("?"));
        }

        String extension = MimeTypeMap.getFileExtensionFromUrl(cleanUrl);
        if (extension != null && !extension.isEmpty()) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType != null) {
                return mimeType;
            }
        }

        // 尝试从URL路径中提取文件扩展名
        if (cleanUrl.lastIndexOf(".") != -1) {
            extension = cleanUrl.substring(cleanUrl.lastIndexOf(".") + 1);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType != null) {
                return mimeType;
            }
        }

        return "application/octet-stream";
    }

    public static String ensureCorrectExtension(String fileName, String mimeType) {
        // 如果文件名没有扩展名或文件扩展名与MIME类型不匹配，添加正确的扩展名
        String extension = null;
        int lastDot = fileName.lastIndexOf(".");

        if (lastDot != -1) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }

        // 从MIME类型获取预期的扩展名
        String expectedExtension = getExtensionFromMimeType(mimeType);

        // 如果没有扩展名或扩展名不匹配预期扩展名，则修正文件名
        if (extension == null || !extension.equals(expectedExtension)) {
            if (lastDot != -1) {
                // 替换现有扩展名
                fileName = fileName.substring(0, lastDot + 1) + expectedExtension;
            } else {
                // 添加扩展名
                fileName = fileName + "." + expectedExtension;
            }
        }

        return fileName;
    }

    public static String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.equals("application/octet-stream")) {
            return "bin";
        }

        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension != null) {
            return extension;
        }

        // 针对常见的MIME类型提供备用扩展名
        switch (mimeType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "audio/mpeg":
                return "mp3";
            case "video/mp4":
                return "mp4";
            case "application/pdf":
                return "pdf";
            default:
                // 尝试从MIME类型字符串中提取扩展名
                if (mimeType.contains("/")) {
                    String subtype = mimeType.split("/")[1];
                    if (subtype.contains("+")) {
                        subtype = subtype.split("\\+")[0];
                    }
                    if (!subtype.equals("octet-stream")) {
                        return subtype;
                    }
                }
                return "bin";
        }
    }
}