package com.android.medialib;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView videoEditor;
    private ImageView audioEditor;
    private ImageView videoCompressor;
    private ImageView audioCompressor;

    static Uri uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        videoEditor = findViewById(R.id.videoEditor);
        audioEditor = findViewById(R.id.audioEditor);
        videoCompressor = findViewById(R.id.videoCompressor);
        audioCompressor = findViewById(R.id.audioCompressor);

        videoEditor.setOnClickListener(this);
        audioEditor.setOnClickListener(this);
        videoCompressor.setOnClickListener(this);
        audioCompressor.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.videoEditor: {
                getVideo();
                break;
            }
            case R.id.audioEditor: {

            }
            case R.id.videoCompressor: {

            }
            case R.id.audioCompressor: {

            }
            default: {
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 210) {
                Intent intent = new Intent(this, VideoEditor.class);
                uri = data.getData();
                startActivity(intent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), 210);
    }
}