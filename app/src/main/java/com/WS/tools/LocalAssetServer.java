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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.URLConnection;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class LocalAssetServer {
    private static AsyncHttpServer server;
    // 初始端口号
    private static int port = 8270;
    private static Context appContext;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "local_server_channel";
    private static NotificationManager notificationManager;

    public static void start(Context context) {
        if (server != null) return;

        appContext = context.getApplicationContext();
        server = new AsyncHttpServer();

        // 静态资源路由
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

        // 探测一个可用端口
        port = findAvailablePort(port);

        // 监听所有接口上的这个可用端口
        server.listen(port);

        // 显示通知
        showServerNotification();
    }

    public static void stop() {
        if (server != null) {
            server.stop();
            server = null;

            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            }
        }
    }

    public static String getLocalUrl() {
        return "http://localhost:" + port + "/";
    }

    public static String getLocalNetworkUrl() {
        String ipAddress = getLocalIpAddress();
        if (ipAddress != null) {
            return "http://" + ipAddress + ":" + port + "/";
        } else {
            return getLocalUrl();
        }
    }

    private static String getLocalIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                    ipInt = Integer.reverseBytes(ipInt);
                }
                byte[] ipByteArray = BigInteger.valueOf(ipInt).toByteArray();
                InetAddress ipAddress = InetAddress.getByAddress(ipByteArray);
                return ipAddress.getHostAddress();
            }
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual() || ni.isPointToPoint()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void showServerNotification() {
        notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "本地服务器",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("显示本地服务器地址");
            notificationManager.createNotificationChannel(channel);
        }

        String serverUrl = getLocalNetworkUrl();

        Intent copyIntent = new Intent(appContext, NotificationActionReceiver.class);
        copyIntent.setAction("COPY_URL");
        copyIntent.putExtra("URL", serverUrl);
        PendingIntent copyPendingIntent = PendingIntent.getBroadcast(
                appContext, 0, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl));
        PendingIntent browserPendingIntent = PendingIntent.getActivity(
                appContext, 0, browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("局域网联机运行中")
                .setContentText("访问地址: " + serverUrl)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_save, "复制地址", copyPendingIntent)
                .setContentIntent(browserPendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void updateServerNotification() {
        if (appContext != null && server != null) {
            showServerNotification();
        }
    }

    /**
     * 从 startPort 开始，依次 +1，直到找到一个可用端口（<=65535），找不到则返回 startPort 本身。
     */
    private static int findAvailablePort(int startPort) {
        int p = startPort;
        while (p <= 65535) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(p);
                ss.setReuseAddress(true);
                return p;
            } catch (IOException e) {
                p++;
            } finally {
                if (ss != null) {
                    try { ss.close(); } catch (IOException ignored) {}
                }
            }
        }
        // 如果一直没找到，则退回原端口
        return startPort;
    }
}