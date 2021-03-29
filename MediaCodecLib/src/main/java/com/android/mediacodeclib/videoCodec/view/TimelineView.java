package com.android.mediacodeclib.videoCodec.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.mediacodeclib.R;
import com.android.mediacodeclib.videoCodec.utils.BackgroundExecutor;
import com.android.mediacodeclib.videoCodec.utils.UiThreadExecutor;

public class TimelineView extends View {

    private Uri mVideoUri;
    private int mHeightView;
    private LongSparseArray<Bitmap> mFramesList = null;

    public TimelineView(Context context) {
        super(context);
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initialize() {
        mHeightView = getContext().getResources().getDimensionPixelOffset(R.dimen.frames_video_height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minWidth = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int width = resolveSizeAndState(minWidth, widthMeasureSpec, 1);

        final int minHeight = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight();
        int height = resolveSizeAndState(minHeight, heightMeasureSpec, 1);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            getBitmap(w);
        }
    }

    private void getBitmap(final int viewWidth) {
        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0L, "") {
                                       @Override
                                       public void execute() {
                                           try {
                                               LongSparseArray<Bitmap> thumbnailList = new LongSparseArray<>();

                                               MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                                               mediaMetadataRetriever.setDataSource(getContext(), mVideoUri);

                                               long videoLengthInMs = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;

                                               final int thumbWidth = mHeightView;
                                               final int thumbHeight = mHeightView;

                                               int numThumbs = (int) Math.ceil(((float) viewWidth) / thumbWidth);

                                               final long interval = videoLengthInMs / numThumbs;

                                               for (int i = 0; i < numThumbs; ++i) {
                                                   Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(i * interval, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                                                   try {
                                                       bitmap = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, false);
                                                   } catch (Exception e) {
                                                       e.printStackTrace();
                                                   }
                                                   thumbnailList.put(i, bitmap);
                                               }

                                               mediaMetadataRetriever.release();
                                               returnBitmaps(thumbnailList);
                                           } catch (final Throwable e) {
                                               Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                                           }
                                       }
                                   }
        );
    }

    private void returnBitmaps(final LongSparseArray<Bitmap> thumbnailList) {
        UiThreadExecutor.runTask("", new Runnable() {
                    @Override
                    public void run() {
                        mFramesList = thumbnailList;
                        invalidate();
                    }
                }
                , 0L);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (mFramesList != null) {
            canvas.save();
            int x = 0;

            for (int i = 0; i < mFramesList.size(); i++) {
                Bitmap bitmap = mFramesList.get(i);

                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, x, 0, null);
                    x = x + bitmap.getWidth();
                }
            }
        }
    }

    public void setVideo(Uri video) {
        this.mVideoUri = video;
    }
}
