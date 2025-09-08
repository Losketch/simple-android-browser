package com.WS.tools;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

public class PermissionsHandler {
    private final Activity activity;
    private static final int STORAGE_PERMISSION_REQUEST = 1;

    public PermissionsHandler(Activity activity) {
        this.activity = activity;
    }
    
    public void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                        STORAGE_PERMISSION_REQUEST);
            }
        }
    }
    
    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "存储权限被拒绝，文件功能可能无法使用！", Toast.LENGTH_LONG).show();
            }
        }
    }
}