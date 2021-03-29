package com.android.mediacodeclib.videoCodec;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.mediacodeclib.R;
import com.android.mediacodeclib.videoCodec.interfaces.OnProgressVideoListener;
import com.android.mediacodeclib.videoCodec.interfaces.OnRangeSeekBarListener;
import com.android.mediacodeclib.videoCodec.interfaces.OnTrimVideoListener;
import com.android.mediacodeclib.videoCodec.utils.BackgroundExecutor;
import com.android.mediacodeclib.videoCodec.utils.TrimVideoUtils;
import com.android.mediacodeclib.videoCodec.utils.UiThreadExecutor;
import com.android.mediacodeclib.videoCodec.view.ProgressBarView;
import com.android.mediacodeclib.videoCodec.view.RangeSeekBarView;
import com.android.mediacodeclib.videoCodec.view.Thumb;
import com.android.mediacodeclib.videoCodec.view.TimelineView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class VideoTrimmer extends FrameLayout {

    private static final String TAG = VideoTrimmer.class.getSimpleName();
    private static final int MIN_TIME_FRAME = 1000;
    private static final int SHOW_PROGRESS = 2;

    private SeekBar mSeekBar;
    private RangeSeekBarView mRangeSeekBar;
    private TimelineView timelineView;

    private View timeInfoContainer;
    private TextView mTextSize;
    private TextView mTextTimeFrame;
    private TextView mTextTime;

    private ProgressBarView mVideoProgressIndicator;
    private Uri sourceVideo;
    private long mOriginSizeFile;
    private String finalPath;

    private VideoView videoView = null;

    private int mDuration = 0;
    private int mTimeVideo = 0;
    private int mStartPosition = 0;
    private int mEndPosition = 0;

    private OnTrimVideoListener onTrimVideoListener;

    private int mMaxDuration;
    private List<OnProgressVideoListener> mListeners;

    public VideoTrimmer(@NonNull Context context) {
        super(context);
    }

    public VideoTrimmer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoTrimmer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public void initialize(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true);

        mSeekBar = findViewById(R.id.handlerTop);
        mVideoProgressIndicator = findViewById(R.id.timeVideoView);
        mRangeSeekBar = findViewById(R.id.timeLineBar);
        timelineView = findViewById(R.id.timeLineView);

        //time indicator views in module
        timeInfoContainer = findViewById(R.id.timeText);
        mTextSize = findViewById(R.id.textSize);
        mTextTimeFrame = findViewById(R.id.textTimeSelection);
        mTextTime = findViewById(R.id.textTime);

        setListeners();
        setMargins();
    }

    private void setListeners() {
        mListeners = new ArrayList<>();
        mListeners.add(new OnProgressVideoListener() {
            @Override
            public void updateProgressBar(int time, int max, float scale) {
                updateVideoProgress(time);
            }
        });
        mListeners.add(mVideoProgressIndicator);

        mRangeSeekBar.addOnRangeSeekBarListener(new OnRangeSeekBarListener() {
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
        mRangeSeekBar.addOnRangeSeekBarListener(mVideoProgressIndicator);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setMargins() {
        int marge = mRangeSeekBar.getThumbs().get(0).getWidthBitmap();
        int widthSeek = mSeekBar.getThumb().getMinimumWidth() / 2;

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mSeekBar.getLayoutParams();
        layoutParams.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
        mSeekBar.setLayoutParams(layoutParams);

        layoutParams = (RelativeLayout.LayoutParams) timelineView.getLayoutParams();
        layoutParams.setMargins(marge, 0, marge, 0);
        timelineView.setLayoutParams(layoutParams);

        layoutParams = (RelativeLayout.LayoutParams) mVideoProgressIndicator.getLayoutParams();
        layoutParams.setMargins(marge, 0, marge, 0);
        mVideoProgressIndicator.setLayoutParams(layoutParams);
    }

    public void setOnTrimVideoListener(@NonNull OnTrimVideoListener listener) {
        this.onTrimVideoListener = listener;
    }

    public void onSave() {
        if (mStartPosition <= 0 && mEndPosition >= mDuration) {
            if (onTrimVideoListener != null) {
                onTrimVideoListener.getResult(sourceVideo);
            }
        } else {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(getContext(), sourceVideo);

            long METADATA_KEY_DURATION = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            final File file = new File(sourceVideo.getPath());

            if (mTimeVideo < MIN_TIME_FRAME) {
                if ((METADATA_KEY_DURATION - mEndPosition) > (MIN_TIME_FRAME - mTimeVideo)) {
                    mEndPosition += (MIN_TIME_FRAME - mTimeVideo);
                } else if (mStartPosition > (MIN_TIME_FRAME - mTimeVideo)) {
                    mStartPosition -= (MIN_TIME_FRAME - mTimeVideo);
                }
            }

            //notify  that video trimming started
            if (onTrimVideoListener != null) {
                onTrimVideoListener.onTrimStarted();
            }

            BackgroundExecutor.execute(
                    new BackgroundExecutor.Task("", 0L, "") {
                        @Override
                        public void execute() {
                            try {
                                TrimVideoUtils.startTrim(file, getDestinationPath(), mStartPosition, mEndPosition, onTrimVideoListener);
                            } catch (final Throwable e) {
                                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                            }
                        }
                    }
            );
        }
    }

    public void setMaxDuration(int value) {

    }

    public void onCancel() {
        if (onTrimVideoListener != null) {
            onTrimVideoListener.cancelAction();
        }
    }

    private void onStopSeekThumbs() {
    }

    private void updateVideoProgress(int time) {
    }

    private String getDestinationPath() {
        if (finalPath == null) {
            File folder = Environment.getExternalStorageDirectory();
            finalPath = folder.getPath() + File.separator;
            Log.d(TAG, "Using default path " + finalPath);
        }
        return finalPath;
    }

    private void onPlayerIndicatorSeekChanged(int progress, boolean fromuser) {
        int duration = (int) ((mDuration * progress) / 1000L);
        if (fromuser) {
            if (duration < mStartPosition) {
                setProgressPosition(mStartPosition);
                duration = mStartPosition;
            } else if (duration > mEndPosition) {
                setProgressPosition(mEndPosition);
                duration = mEndPosition;
            }
            setTimeVideo(duration);
        }
    }

    private void onPlayerIndicatorSeekStart() {

    }

    private void onPlayerIndicatorSeekStop(@NonNull SeekBar seekBar) {
        int duration = (int) ((mDuration * seekBar.getProgress()) / 1000L);
        setTimeVideo(duration);
    }

    private void setSeekBarPosition() {

        if (mDuration >= mMaxDuration) {
            mStartPosition = mDuration / 2 - mMaxDuration / 2;
            mEndPosition = mDuration / 2 + mMaxDuration / 2;

            mRangeSeekBar.setThumbValue(0, (mStartPosition * 100) / mDuration);
            mRangeSeekBar.setThumbValue(1, (mEndPosition * 100) / mDuration);

        } else {
            mStartPosition = 0;
            mEndPosition = mDuration;
        }

        setProgressPosition(mStartPosition);

        mTimeVideo = mDuration;
        mRangeSeekBar.initMaxWidth();
    }

    private void setTimeFrames() {
        String seconds = "s";
        mTextTimeFrame.setText(String.format("%s %s - %s %s", TrimVideoUtils.stringForTime(mStartPosition), seconds, TrimVideoUtils.stringForTime(mEndPosition), seconds));
    }

    private void setTimeVideo(int position) {
        String seconds = "s";
        mTextTime.setText(String.format("%s %s", TrimVideoUtils.stringForTime(position), seconds));
    }

    private void onSeekThumbs(int index, float value) {
        switch (index) {
            case Thumb.LEFT: {
                mStartPosition = (int) ((mDuration * value) / 100L);
                break;
            }
            case Thumb.RIGHT: {
                mEndPosition = (int) ((mDuration * value) / 100L);
                break;
            }
        }
        setProgressPosition(mStartPosition);

        setTimeFrames();
        mTimeVideo = mEndPosition - mStartPosition;
    }

    private void setProgressPosition(int mStartPosition) {
    }

    private void destroy() {
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }

    public void setVideoUri(final Uri path) {
        final long KBInGb = 1045504;
        sourceVideo = path;
        if (mOriginSizeFile == 0) {
            File file = new File(sourceVideo.getPath());

            mOriginSizeFile = file.length();
            long fileSizeInKB = mOriginSizeFile / 1024;

            if (fileSizeInKB >= KBInGb) {
                long fileSizeGb = fileSizeInKB / KBInGb;
                mTextSize.setText(fileSizeGb + " GB");
            } else if (fileSizeInKB >= 1024) {
                long fileSizeMb = fileSizeInKB / 1024;
                mTextSize.setText(fileSizeMb + " MB");
            } else if (fileSizeInKB > 0) {
                mTextSize.setText(fileSizeInKB + " KB");
            }
        }
        timelineView.setVideo(sourceVideo);
    }

    private static class MessageHandler extends Handler {

        @NonNull
        private final WeakReference<VideoTrimmer> mView;

        MessageHandler(VideoTrimmer view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoTrimmer view = mView.get();
            if (view == null) {
                return;
            }
        }
    }
}
