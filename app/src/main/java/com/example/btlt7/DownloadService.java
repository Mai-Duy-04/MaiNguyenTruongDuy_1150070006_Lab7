package com.example.btlt7;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {
    public static boolean isPaused = false;
    public static boolean isCanceled = false;
    private static final int NOTI_ID = 101;
    private String currentUrl;
    private long downloaded = 0;
    private long totalBytes = 0;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (Consts.ACTION_START.equals(action)) {
            currentUrl = intent.getStringExtra("url");
            createNotificationChannel();
            startForeground(NOTI_ID, buildProgressNotification(0));
            new Thread(() -> downloadFile(currentUrl)).start();
        } else if (Consts.ACTION_PAUSE.equals(action)) {
            isPaused = true;
            updateNotification("Tạm dừng");
        } else if (Consts.ACTION_RESUME.equals(action)) {
            isPaused = false;
            updateNotification("Tiếp tục...");
        } else if (Consts.ACTION_CANCEL.equals(action)) {
            isCanceled = true;
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    /** ⚙️ Tạm bỏ kiểm tra SSL (chỉ dùng cho lab/test) */
    @SuppressLint("CustomX509TrustManager")
    private void trustAllHosts() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception ignored) {}
    }

    /** 📥 Tiến trình tải file */
    private void downloadFile(String fileUrl) {
        trustAllHosts();
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true); // Fix lỗi 302
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                showError("Lỗi HTTP: " + conn.getResponseCode());
                stopSelf();
                return;
            }

            File output = new File(getFilesDir(), "downloaded_file");
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(output);
            totalBytes = conn.getContentLength();
            byte[] buffer = new byte[4096];
            int bytesRead;
            downloaded = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                if (isCanceled) { out.close(); output.delete(); stopSelf(); return; }
                while (isPaused) { Thread.sleep(200); }

                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                int progress = (int) ((downloaded * 100) / totalBytes);
                updateProgress(progress);
            }

            out.close();
            in.close();
            updateNotification("Tải xong ✅: " + output.getAbsolutePath());
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    /** 🔔 Channel cho Notification */
    @SuppressLint("ObsoleteSdkInt")
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(Consts.CHANNEL_ID, "Download", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(ch);
        }
    }

    /** 🎨 Notification có thanh tiến trình + nút Pause/Cancel */
    private Notification buildProgressNotification(int progress) {
        Intent pauseIntent = new Intent(this, DownloadActionReceiver.class).setAction(Consts.ACTION_PAUSE);
        PendingIntent pausePI = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent cancelIntent = new Intent(this, DownloadActionReceiver.class).setAction(Consts.ACTION_CANCEL);
        PendingIntent cancelPI = PendingIntent.getBroadcast(this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, Consts.CHANNEL_ID)
                .setContentTitle("Download manager")
                .setContentText("Đang tải file...")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, false)
                .addAction(android.R.drawable.ic_media_pause, "Pause", pausePI)
                .addAction(android.R.drawable.ic_delete, "Cancel", cancelPI)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    /** 🔁 Cập nhật thanh progress */
    private void updateProgress(int progress) {
        Notification n = buildProgressNotification(progress);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTI_ID, n);
    }

    /** ✅ Cập nhật trạng thái text */
    private void updateNotification(String msg) {
        Notification n = new NotificationCompat.Builder(this, Consts.CHANNEL_ID)
                .setContentTitle("Download manager")
                .setContentText(msg)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTI_ID, n);
    }

    /** ❌ Báo lỗi */
    private void showError(String msg) {
        Notification n = new NotificationCompat.Builder(this, Consts.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download manager")
                .setContentText(msg)
                .build();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTI_ID, n);
    }
}
