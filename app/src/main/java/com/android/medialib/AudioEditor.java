package com.android.medialib;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.android.mediacodeclib.AudioCodec.AudioCutTrimmer;
import com.android.mediacodeclib.VideoCodec.interfaces.OnTrimVideoListener;
import com.android.mediacodeclib.VideoCodec.interfaces.OnVideoCutListener;

public class AudioEditor extends AppCompatActivity implements OnTrimVideoListener, OnVideoCutListener {
    private Uri uri;
    private AudioCutTrimmer trimmer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_editor);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("KEY")) {
            String data = extras.getString("KEY");
            uri = Uri.parse(data);
        }
        trimmer = findViewById(R.id.audioEditorTrimmer);

        String[] permissios = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (hasPermissions(permissios)) {
            setTrimmer();
        } else {
            ActivityCompat.requestPermissions((Activity) this, permissios, 201);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setTrimmer();
    }

    private void setTrimmer() {
        trimmer.init(this);
        trimmer.setAudioURI(uri);
        trimmer.setAudioInformationVisibility(true);
        trimmer.setMaxDuration(30);
        trimmer.setOnTrimVideoListener(this);
        trimmer.setOnVideoCutListener(this);
    }


    private boolean hasPermissions(String[] permissions) {
        for (int i = 0; i < permissions.length; i++) {
            if (ActivityCompat.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(Uri uri) {

    }

    @Override
    public void cancelAction() {

    }

    @Override
    public void onError(String message) {

    }

    @Override
    public void onVideoPrepared() {

    }
}