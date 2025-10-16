package com.example.btlt7;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private EditText edtUrl;
    private final ActivityResultLauncher<String> reqNotiPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtUrl = findViewById(R.id.edtUrl);
        Button btnDownload = findViewById(R.id.btnDownload);
        Button btnPause = findViewById(R.id.btnPause);
        Button btnResume = findViewById(R.id.btnResume);
        Button btnCancel = findViewById(R.id.btnCancel);

        // Android 13+ xin quyền notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            reqNotiPerm.launch(Manifest.permission.POST_NOTIFICATIONS);

        btnDownload.setOnClickListener(v -> startAction(Consts.ACTION_START));
        btnPause.setOnClickListener(v -> startAction(Consts.ACTION_PAUSE));
        btnResume.setOnClickListener(v -> startAction(Consts.ACTION_RESUME));
        btnCancel.setOnClickListener(v -> startAction(Consts.ACTION_CANCEL));
    }

    private void startAction(String action) {
        String url = ((EditText) findViewById(R.id.edtUrl)).getText().toString().trim();
        if (action.equals(Consts.ACTION_START) && url.isEmpty()) {
            Toast.makeText(this, "Nhập URL trước đã!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent svc = new Intent(this, DownloadService.class);
        svc.setAction(action);
        if (action.equals(Consts.ACTION_START)) svc.putExtra("url", url);
        startService(svc);
    }
}
