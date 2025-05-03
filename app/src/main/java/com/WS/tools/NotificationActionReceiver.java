package com.WS.tools;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("COPY_URL")) {
            String url = intent.getStringExtra("URL");
            if (url != null) {
                // 复制URL到剪贴板
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("服务器地址", url);
                clipboard.setPrimaryClip(clip);
                
                // 显示提示
                Toast.makeText(context, "服务器地址已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        }
    }
}