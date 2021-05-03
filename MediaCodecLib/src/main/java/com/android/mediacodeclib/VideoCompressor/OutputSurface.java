package com.android.mediacodeclib.VideoCompressor;

import android.graphics.SurfaceTexture;
import android.view.Surface;

public class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private final Object mFrameSyncObject = new Object();
    private boolean mFrameAvailable;
    private TextureRenderer mTextureRender;

    public OutputSurface() {
        setup();
    }
    private void setup() {
        mTextureRender = new TextureRenderer();
        mTextureRender.surfaceCreated();
        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurface = new Surface(mSurfaceTexture);
    }

    public void release() {
        mSurface.release();

        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;

        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {

                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        mTextureRender.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
    }


    public void drawImage() {
        mTextureRender.drawFrame(mSurfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }
}
