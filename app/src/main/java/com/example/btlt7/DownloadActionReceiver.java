package com.example.btlt7;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent svc = new Intent(context, DownloadService.class);
        svc.setAction(intent.getAction());
        context.startService(svc);
    }
}
