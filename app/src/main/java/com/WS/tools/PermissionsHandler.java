package com.WS.tools;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PermissionsHandler {
    private final Activity activity;
    private static final int STORAGE_PERMISSION_REQUEST = 1;
    public static final int MANAGE_STORAGE_PERMISSION_REQUEST = 2;
    private static final int MEDIA_PERMISSION_REQUEST = 3;

    public PermissionsHandler(Activity activity) {
        this.activity = activity;
    }
    
    public void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要媒体权限
            requestMediaPermissions();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 需要 MANAGE_EXTERNAL_STORAGE 权限
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-10 需要 WRITE_EXTERNAL_STORAGE 权限
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                        STORAGE_PERMISSION_REQUEST);
            }
        }
    }

    private void requestManageStoragePermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST);
        } catch (Exception e) {
            Toast.makeText(activity, "无法打开权限设置页面，请手动在系统设置中授予存储权限", Toast.LENGTH_LONG).show();
        }
    }

    private void requestMediaPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (activity.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (activity.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        }
        
        if (!permissions.isEmpty()) {
            activity.requestPermissions(
                    permissions.toArray(new String[0]),
                    MEDIA_PERMISSION_REQUEST);
        }
    }
    
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_REQUEST || requestCode == MEDIA_PERMISSION_REQUEST) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(activity, "存储权限被拒绝，文件功能可能无法使用！", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    public void onActivityResult(int requestCode) {
        if (requestCode == MANAGE_STORAGE_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Toast.makeText(activity, "存储权限被拒绝，文件功能可能无法使用！", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "存储权限已授予", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}