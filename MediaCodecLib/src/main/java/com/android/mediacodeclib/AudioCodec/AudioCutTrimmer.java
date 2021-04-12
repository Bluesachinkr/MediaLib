package com.android.mediacodeclib.AudioCodec;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.mediacodeclib.AudioCodec.utils.TrimAudio;
import com.android.mediacodeclib.AudioCodec.view.AudioTimelineView;
import com.android.mediacodeclib.VideoCodec.interfaces.OnProgressVideoListener;
import com.android.mediacodeclib.VideoCodec.interfaces.OnRangeSeekBarListener;
import com.android.mediacodeclib.VideoCodec.interfaces.OnTrimVideoListener;
import com.android.mediacodeclib.VideoCodec.interfaces.OnVideoCutListener;
import com.android.mediacodeclib.VideoCodec.utils.BackgroundExecutor;
import com.android.mediacodeclib.VideoCodec.utils.UiThreadExecutor;
import com.android.mediacodeclib.VideoCodec.view.ProgressBarView;
import com.android.mediacodeclib.VideoCodec.view.RangeSeekBarView;
import com.android.mediacodeclib.VideoCodec.view.Thumb;
import com.android.videoeditpro.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.android.mediacodeclib.VideoCodec.utils.TrimVideoUtils.stringForTime;

public class AudioCutTrimmer extends FrameLayout {

    private static final int SHOW_PROGRESS = 2;
    private static final String TAG = AudioCutTrimmer.class.getName();

    private SeekBar mHolderTopView;
    private RangeSeekBarView mRangeSeekBarView;
    private View mTimeInfoContainer;
    private ImageView mPlayView;
    private TextView mTextSize;
    private TextView mTextTimeFrame;
    private TextView mTextTime;
    private AudioTimelineView mTimeLineView;

    private ProgressBarView mVideoProgressIndicator;
    private MediaPlayer mediaPlayer;
    private Uri mSrc;
    private String mFinalPath;

    private int mMaxDuration;
    private List<OnProgressVideoListener> mListeners;

    private OnTrimVideoListener mOnTrimVideoListener;
    private OnVideoCutListener mOnVideoCutListener;

    private int mDuration = 0;
    private int mTimeAudio = 0;
    private Context context;
    private int mStartPosition = 0;
    private int mEndPosition = 0;

    private long mOriginSizeFile;
    private boolean mResetSeekBar = true;
    private final MessageHandler mMessageHandler = new MessageHandler(this);

    public AudioCutTrimmer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioCutTrimmer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.audio_trim, this, true);

        mHolderTopView = ((SeekBar) findViewById(R.id.handlerTopAudio));
        mVideoProgressIndicator = ((ProgressBarView) findViewById(R.id.timeAudioView));
        mRangeSeekBarView = ((RangeSeekBarView) findViewById(R.id.timeLineBarAudio));
        mPlayView = ((ImageView) findViewById(R.id.icon_audio_play));
        mTimeInfoContainer = findViewById(R.id.timeTextAudio);
        mTextSize = ((TextView) findViewById(R.id.textSizeAudio));
        mTextTimeFrame = ((TextView) findViewById(R.id.textTimeSelectionAudio));
        mTextTime = ((TextView) findViewById(R.id.textTimeAudio));
        mTimeLineView = ((AudioTimelineView) findViewById(R.id.audioTimeline));
        mediaPlayer = new MediaPlayer();
        setUpListeners();
        setUpMargins();
    }

    private void setUpListeners() {
        mListeners = new ArrayList<>();
        mListeners.add(new OnProgressVideoListener() {
            @Override
            public void updateProgress(int time, int max, float scale) {
                updateVideoProgress(time);
            }
        });
        mListeners.add(mVideoProgressIndicator);
        findViewById(R.id.btCancelAudio)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                onCancelClicked();
                            }
                        }
                );

        findViewById(R.id.btSaveAudio)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                onSaveClicked();
                            }
                        }
                );

        mPlayView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onClickVideoPlayPause();
            }
        });

        mRangeSeekBarView.addOnRangeSeekBarListener(new OnRangeSeekBarListener() {
            @Override
            public void onCreate(RangeSeekBarView rangeSeekBarView, int index, float value) {
            }

            @Override
            public void onSeek(RangeSeekBarView rangeSeekBarView, int index, float value) {
                onSeekThumbs(index, value);
            }

            @Override
            public void onSeekStart(RangeSeekBarView rangeSeekBarView, int index, float value) {
            }

            @Override
            public void onSeekStop(RangeSeekBarView rangeSeekBarView, int index, float value) {
                onStopSeekThumbs();
            }
        });
        mRangeSeekBarView.addOnRangeSeekBarListener(mVideoProgressIndicator);

        mHolderTopView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onPlayerIndicatorSeekChanged(progress, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStart();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStop(seekBar);
            }
        });

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                onVideoPrepared(mp);
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onVideoCompleted();
            }
        });
    }

    private void setUpMargins() {
        int marge = mRangeSeekBarView.getThumbs().get(0).getWidthBitmap();
        int widthSeek = mHolderTopView.getThumb().getMinimumWidth() / 2;

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mHolderTopView.getLayoutParams();
        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
        mHolderTopView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mTimeLineView.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mTimeLineView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mVideoProgressIndicator.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mVideoProgressIndicator.setLayoutParams(lp);
    }

    private void seekTo(int position) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        mediaPlayer.seekTo(position);
    }

    private void onSaveClicked() {
        if (mStartPosition <= 0 && mEndPosition >= mDuration) {
            if (mOnTrimVideoListener != null)
                mOnTrimVideoListener.getResult(mSrc);
        } else {
            mPlayView.setVisibility(View.VISIBLE);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }

            final File file = new File(mSrc.getPath());

            if (mOnTrimVideoListener != null)
                mOnTrimVideoListener.onTrimStarted();

            BackgroundExecutor.execute(
                    new BackgroundExecutor.Task("", 0L, "") {
                        @Override
                        public void execute() {
                            try {
                                String directory = context.getExternalFilesDir(null).getAbsolutePath();
                                directory += "/AudioTrimmer";
                                File d = new File(directory);
                                if (!d.exists()) {
                                    d.mkdirs();
                                }
                                File outputFile = new File(d.getAbsolutePath() + "/trimmedAudio" + ".mp3");
                                if (outputFile.exists()) {
                                    outputFile.delete();
                                }
                                outputFile.createNewFile();
                                TrimAudio trimmer = new TrimAudio();
                                /*trimmer.ReadFile(file);
                                trimmer.WriteFile(outputFile, mStartPosition, mEndPosition);*/

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );
        }
    }

    private void onClickVideoPlayPause() {
        if (mediaPlayer.isPlaying()) {
            mPlayView.setImageResource(R.drawable.play_button);
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            mediaPlayer.pause();
        } else {
            mPlayView.setImageResource(R.drawable.pause_btn);

            if (mResetSeekBar) {
                mResetSeekBar = false;
                seekTo(mStartPosition);
            }

            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS);
            mediaPlayer.start();
        }
    }

    private void onCancelClicked() {
        mediaPlayer.stop();
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener.cancelAction();
        }
    }

    private String getDestinationPath() {
        if (mFinalPath == null) {
            File folder = Environment.getExternalStorageDirectory();
            mFinalPath = folder.getPath() + File.separator;
            Log.d(TAG, "Using default path " + mFinalPath);
        }
        return mFinalPath;
    }

    private void onPlayerIndicatorSeekChanged(int progress, boolean fromUser) {

        int duration = (int) ((mDuration * progress) / 1000L);

        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition);
                duration = mStartPosition;
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition);
                duration = mEndPosition;
            }
            setTimeVideo(duration);
        }
    }

    private void onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        mPlayView.setVisibility(View.VISIBLE);
        notifyProgressUpdate(false);
    }

    private void onPlayerIndicatorSeekStop(@NonNull SeekBar seekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        mPlayView.setVisibility(View.VISIBLE);

        int duration = (int) ((mDuration * seekBar.getProgress()) / 1000L);
        seekTo(duration);
        setTimeVideo(duration);
        notifyProgressUpdate(false);
    }

    private void onVideoPrepared(@NonNull MediaPlayer mp) {
        mDuration = mediaPlayer.getDuration();
        setSeekBarPosition();

        setTimeFrames();
        setTimeVideo(0);

        if (mOnVideoCutListener != null) {
            mOnVideoCutListener.onVideoPrepared();
        }
    }

    private void setSeekBarPosition() {

        mStartPosition = 0;
        mEndPosition = mDuration;

        setProgressBarPosition(mStartPosition);
        seekTo(mStartPosition);

        mTimeAudio = mDuration;
        mRangeSeekBarView.initMaxWidth();
    }

    private void setTimeFrames() {
        String seconds = getContext().getString(R.string.short_seconds);
        mTextTimeFrame.setText(String.format("%s %s - %s %s", stringForTime(mStartPosition), seconds, stringForTime(mEndPosition), seconds));
    }

    private void setTimeVideo(int position) {
        String seconds = getContext().getString(R.string.short_seconds);
        mTextTime.setText(String.format("%s %s", stringForTime(position), seconds));
    }

    private void onSeekThumbs(int index, float value) {
        switch (index) {
            case Thumb.LEFT: {
                mStartPosition = (int) ((mDuration * value) / 100L);
                seekTo(mStartPosition);
                break;
            }
            case Thumb.RIGHT: {
                mEndPosition = (int) ((mDuration * value) / 100L);
                break;
            }
        }
        setProgressBarPosition(mStartPosition);

        setTimeFrames();
        mTimeAudio = mEndPosition - mStartPosition;
    }

    private void onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        mPlayView.setVisibility(View.VISIBLE);
    }

    private void onVideoCompleted() {
        seekTo(mStartPosition);
    }

    private void notifyProgressUpdate(boolean all) {
        if (mDuration == 0) return;

        int position = mediaPlayer.getCurrentPosition();
        if (all) {
            for (OnProgressVideoListener item : mListeners) {
                item.updateProgress(position, mDuration, ((position * 100) / mDuration));
            }
        } else {
            mListeners.get(1).updateProgress(position, mDuration, ((position * 100) / mDuration));
        }
    }

    private void updateVideoProgress(int time) {
        if (mediaPlayer == null) {
            return;
        }

        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            mPlayView.setVisibility(View.VISIBLE);
            mResetSeekBar = true;
            return;
        }

        if (mHolderTopView != null) {
            setProgressBarPosition(time);
        }
        setTimeVideo(time);
    }

    private void setProgressBarPosition(int position) {
        if (mDuration > 0) {
            long pos = 1000L * position / mDuration;
            mHolderTopView.setProgress((int) pos);
        }
    }

    public void setAudioInformationVisibility(boolean visible) {
        mTimeInfoContainer.setVisibility(visible ? VISIBLE : GONE);
    }


    public void setOnTrimVideoListener(OnTrimVideoListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }


    public void setOnVideoCutListener(OnVideoCutListener onVideoCutListener) {
        mOnVideoCutListener = onVideoCutListener;
    }

    public void setDestinationPath(final String finalPath) {
        mFinalPath = finalPath;
        Log.d(TAG, "Setting custom path " + mFinalPath);
    }

    public void destroy() {
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }

    public void setMaxDuration(int maxDuration) {
        mMaxDuration = maxDuration * 1000;
    }

    public void setAudioURI(final Uri videoURI) {
        mSrc = videoURI;

        if (mOriginSizeFile == 0) {
            File file = new File(mSrc.getPath());

            mOriginSizeFile = file.length();
            long fileSizeInKB = mOriginSizeFile / 1024;

            if (fileSizeInKB > 1000) {
                long fileSizeInMB = fileSizeInKB / 1024;
                mTextSize.setText(fileSizeInMB + "MB");
            } else {
                mTextSize.setText(fileSizeInKB + "KB");
            }
            try {
                mediaPlayer.setDataSource(mSrc.getPath());
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setTimeline(file);
        }
    }

    private void setTimeline(File file) {
        byte[] audioBytes = fileToBytes(file);
        mTimeLineView.updateVisualizer(audioBytes);
    }

    public static byte[] fileToBytes(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private static class MessageHandler extends Handler {

        @NonNull
        private final WeakReference<AudioCutTrimmer> mView;

        MessageHandler(AudioCutTrimmer view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            AudioCutTrimmer view = mView.get();
            if (view == null || view.mediaPlayer == null) {
                return;
            }

            view.notifyProgressUpdate(true);
            if (view.mediaPlayer.isPlaying()) {
                sendEmptyMessageDelayed(0, 10);
            }
        }
    }
}
