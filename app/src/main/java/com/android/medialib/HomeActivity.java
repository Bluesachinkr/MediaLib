package com.android.medialib;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.android.mediacodeclib.VideoCodec.VideoCutTrimmer;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private int VIDEO_REQUEST_CODE = 201;
    private int AUDIO_REQUEST_CODE = 202;
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
                Intent intent = getVideo();
                startActivityForResult(Intent.createChooser(intent, "Select Video"), VIDEO_REQUEST_CODE);
                break;
            }
            case R.id.audioEditor: {
                getAudio();
                break;
            }
            case R.id.videoCompressor: {
                Intent intent = getVideo();
                startActivityForResult(Intent.createChooser(intent, "Select Video"), 300);
                break;

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
        if (resultCode == RESULT_OK && requestCode == VIDEO_REQUEST_CODE) {
            Intent intent = new Intent(this, VideoEditor.class);
            uri = data.getData();
            startActivity(intent);
        } else if (resultCode == RESULT_OK && requestCode == AUDIO_REQUEST_CODE) {
            Intent intent = new Intent(this, AudioEditor.class);
            Uri uri = data.getData();
            intent.putExtra("KEY", uri.getPath());
            startActivity(intent);
        } else if (resultCode == RESULT_OK && requestCode == 300) {
            Intent intent = new Intent(this, CompressorActivity.class);
            Uri uri = data.getData();
            intent.putExtra("KEY", uri.toString());
            startActivity(intent);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getAudio() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), AUDIO_REQUEST_CODE);
    }

    private Intent getVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return intent;
    }
}