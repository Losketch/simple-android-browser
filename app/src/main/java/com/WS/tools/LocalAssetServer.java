package com.WS.tools;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLConnection;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class LocalAssetServer {
    private static AsyncHttpServer server;
    private static int port = 8270;
    private static Context appContext;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "local_server_channel";
    private static NotificationManager notificationManager;

    public static void start(Context context) {
        if (server != null) return;

        appContext = context.getApplicationContext();
        server = new AsyncHttpServer();
        server.get("/.*", (request, response) -> {
            try {
                String path = request.getPath().replaceFirst("/", "");
                if (path.isEmpty()) path = "index.html";

                InputStream is = context.getAssets().open(path);
                String mime = URLConnection.guessContentTypeFromName(path);
                response.sendStream(is, is.available());
                response.setContentType(mime != null ? mime : "text/plain");
            }
            catch (Exception e) {
                response.code(404);
                response.end();
            }
        });

        // 监听所有接口上的端口，而不仅仅是localhost
        server.listen(port);

        // 显示永久通知
        showServerNotification();
    }

    public static void stop() {
        if (server != null) {
            server.stop();
            server = null;

            // 移除通知
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            }
        }
    }

    public static String getLocalUrl() {
        return "http://localhost:" + port + "/";
    }

    // 获取设备在局域网中的IP地址，供局域网内其他设备访问
    public static String getLocalNetworkUrl() {
        String ipAddress = getLocalIpAddress();
        if (ipAddress != null) {
            return "http://" + ipAddress + ":" + port + "/";
        } else {
            return getLocalUrl(); // 如果获取不到IP地址，返回localhost地址
        }
    }

    // 获取设备的局域网IP地址
    private static String getLocalIpAddress() {
        try {
            // 尝试获取WIFI IP地址
            WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                // 转换字节顺序
                if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                    ipInt = Integer.reverseBytes(ipInt);
                }

                byte[] ipByteArray = BigInteger.valueOf(ipInt).toByteArray();
                InetAddress ipAddress = InetAddress.getByAddress(ipByteArray);
                return ipAddress.getHostAddress();
            }

            // 如果WIFI未连接，尝试获取其他网络接口
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                // 跳过回环接口、虚拟接口等
                if (!networkInterface.isUp() || networkInterface.isLoopback() ||
                        networkInterface.isVirtual() || networkInterface.isPointToPoint()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') < 0) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 显示永久通知，展示服务器地址
    private static void showServerNotification() {
        notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // 为Android 8.0及以上创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "本地服务器",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("显示本地服务器地址");
            notificationManager.createNotificationChannel(channel);
        }

        String serverUrl = getLocalNetworkUrl();

        // 创建点击复制地址的Intent
        Intent copyIntent = new Intent(appContext, NotificationActionReceiver.class);
        copyIntent.setAction("COPY_URL");
        copyIntent.putExtra("URL", serverUrl);
        PendingIntent copyPendingIntent = PendingIntent.getBroadcast(
                appContext, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 创建点击打开浏览器的Intent
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl));
        PendingIntent browserPendingIntent = PendingIntent.getActivity(
                appContext, 0, browserIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // 替换为你的应用图标
                .setContentTitle("局域网联机运行中")
                .setContentText("访问地址: " + serverUrl)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // 设置为持续通知
                .addAction(android.R.drawable.ic_menu_save, "复制地址", copyPendingIntent)
                .setContentIntent(browserPendingIntent);

        // 显示通知
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    // 更新通知中的服务器地址（当IP地址变化时可调用）
    public static void updateServerNotification() {
        if (appContext != null && server != null) {
            showServerNotification();
        }
    }
}