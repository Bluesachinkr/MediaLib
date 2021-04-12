package com.android.mediacodeclib.AudioCodec.utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.FileUtils;
import android.os.RecoverySystem;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Objects;

public class TrimAudio {
    private File mInputFile;
    private String mFileType;
    private int mFileSize;
    private int mSampleRate;
    private int mChannels;
    private ByteBuffer mDecodedBytes;
    private ShortBuffer mDecodedSamples;
    private int mNumSamples;
    private float mAvgBitRate;
    private int mNumFrames;
    private int[] mFrameGains;
    private int[] mFrameLens;
    private int[] mFrameOffsets;
    private ProgressListener mProgressListener = null;

    private void setProgressListener(ProgressListener progressListener) {
        mProgressListener = progressListener;
    }

    public interface ProgressListener {
        boolean reportProgress(double fractionComplete);
    }

    public void ReadFile(File inputFile)
            throws java.io.FileNotFoundException,
            IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaFormat format = null;
        int i;

        mInputFile = inputFile;
        String[] components = mInputFile.getPath().split("\\.");
        mFileType = components[components.length - 1];
        mFileSize = (int) mInputFile.length();
        extractor.setDataSource(mInputFile.getPath());
        int numTracks = extractor.getTrackCount();

        for (i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                extractor.selectTrack(i);
                break;
            }
        }
        mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int expectedNumSamples =
                (int) ((format.getLong(MediaFormat.KEY_DURATION) / 1000000.f) * mSampleRate + 0.5f);

        MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        codec.configure(format, null, null, 0);
        codec.start();

        int decodedSamplesSize = 0;
        byte[] decodedSamples = null;
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        int sample_size;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long presentation_time;
        int tot_size_read = 0;
        boolean done_reading = false;

        mDecodedBytes = ByteBuffer.allocate(1 << 20);
        Boolean firstSampleData = true;
        /*while (true) {
            int inputBufferIndex = codec.dequeueInputBuffer(100);
            if (!done_reading && inputBufferIndex >= 0) {
                sample_size = extractor.readSampleData(inputBuffers[inputBufferIndex], 0);
                if (firstSampleData
                        && format.getString(MediaFormat.KEY_MIME).equals("audio/mp4a-latm")
                        && sample_size == 2) {
                    extractor.advance();
                    tot_size_read += sample_size;
                } else if (sample_size < 0) {
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    done_reading = true;
                } else {
                    presentation_time = extractor.getSampleTime();
                    codec.queueInputBuffer(inputBufferIndex, 0, sample_size, presentation_time, 0);
                    extractor.advance();
                    tot_size_read += sample_size;
                    if (mProgressListener != null) {
                        if (!mProgressListener.reportProgress((float) (tot_size_read) / mFileSize)) {
                            extractor.release();
                            extractor = null;
                            codec.stop();
                            codec.release();
                            codec = null;
                            return;
                        }
                    }
                }
                firstSampleData = false;
            }

            int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
            if (outputBufferIndex >= 0 && info.size > 0) {
                if (decodedSamplesSize < info.size) {
                    decodedSamplesSize = info.size;
                    decodedSamples = new byte[decodedSamplesSize];
                }
                outputBuffers[outputBufferIndex].get(decodedSamples, 0, info.size);
                outputBuffers[outputBufferIndex].clear();
                if (mDecodedBytes.remaining() < info.size) {
                    int position = mDecodedBytes.position();
                    int newSize = (int) ((position * (1.0 * mFileSize / tot_size_read)) * 1.2);
                    if (newSize - position < info.size + 5 * (1 << 20)) {
                        newSize = position + info.size + 5 * (1 << 20);
                    }
                    ByteBuffer newDecodedBytes = null;
                    int retry = 10;
                    while (retry > 0) {
                        try {
                            newDecodedBytes = ByteBuffer.allocate(newSize);
                            break;
                        } catch (OutOfMemoryError oome) {
                            retry--;
                        }
                    }
                    if (retry == 0) {
                        break;
                    }
                    mDecodedBytes.rewind();
                    newDecodedBytes.put(mDecodedBytes);
                    mDecodedBytes = newDecodedBytes;
                    mDecodedBytes.position(position);
                }
                mDecodedBytes.put(decodedSamples, 0, info.size);
                codec.releaseOutputBuffer(outputBufferIndex, false);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = codec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    || (mDecodedBytes.position() / (2 * mChannels)) >= expectedNumSamples) {
                break;
            }
        }*/
        mNumSamples = mDecodedBytes.position() / (mChannels * 2);  // One sample = 2 bytes.
        mDecodedBytes.rewind();
        mDecodedBytes.order(ByteOrder.LITTLE_ENDIAN);
        mDecodedSamples = mDecodedBytes.asShortBuffer();
        mAvgBitRate = (int) ((mFileSize * 8) * ((float) mSampleRate / mNumSamples) / 1000);

        extractor.release();
        extractor = null;
        codec.stop();
        codec.release();
        codec = null;

        // Temporary hack to make it work with the old version.
        mNumFrames = mNumSamples / 1024;
        if (mNumSamples % 1024 != 0) {
            mNumFrames++;
        }
        mFrameGains = new int[mNumFrames];
        mFrameLens = new int[mNumFrames];
        mFrameOffsets = new int[mNumFrames];
        int j;
        int gain, value;
        int frameLens = (int) ((1000 * mAvgBitRate / 8) *
                ((float) 1024 / mSampleRate));
        for (i = 0; i < mNumFrames; i++) {
            gain = -1;
            for (j = 0; j < 1024; j++) {
                value = 0;
                for (int k = 0; k < mChannels; k++) {
                    if (mDecodedSamples.remaining() > 0) {
                        value += Math.abs(mDecodedSamples.get());
                    }
                }
                value /= mChannels;
                if (gain < value) {
                    gain = value;
                }
            }
            mFrameGains[i] = (int) Math.sqrt(gain);
            mFrameLens[i] = frameLens;
            mFrameOffsets[i] = (int) (i * (1000 * mAvgBitRate / 8) *
                    ((float) 1024 / mSampleRate));
        }
        mDecodedSamples.rewind();
    }

    public void WriteFile(File outputFile, float startTime, float endTime)
            throws IOException {
        int startOffset = (int) (startTime * mSampleRate) * 2 * mChannels;
        int numSamples = (int) ((endTime - startTime) * mSampleRate);
        int numChannels = (mChannels == 1) ? 2 : mChannels;

        String mimeType = "audio/mp4a-latm";
        int bitrate = 64000 * numChannels;
        MediaCodec codec = MediaCodec.createEncoderByType(mimeType);
        MediaFormat format = MediaFormat.createAudioFormat(mimeType, mSampleRate, numChannels);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();

        int estimatedEncodedSize = (int) ((endTime - startTime) * (bitrate / 8) * 1.1);
        ByteBuffer encodedBytes = ByteBuffer.allocate(estimatedEncodedSize);
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean done_reading = false;
        long presentation_time = 0;

        int frame_size = 1024;
        byte buffer[] = new byte[frame_size * numChannels * 2];
        mDecodedBytes.position(startOffset);
        numSamples += (2 * frame_size);
        int tot_num_frames = 1 + (numSamples / frame_size);
        if (numSamples % frame_size != 0) {
            tot_num_frames++;
        }
        int[] frame_sizes = new int[tot_num_frames];
        int num_out_frames = 0;
        int num_frames = 0;
        int num_samples_left = numSamples;
        int encodedSamplesSize = 0;
        byte[] encodedSamples = null;
        while (true) {
            int inputBufferIndex = codec.dequeueInputBuffer(100);
            if (!done_reading && inputBufferIndex >= 0) {
                if (num_samples_left <= 0) {
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    done_reading = true;
                } else {
                    inputBuffers[inputBufferIndex].clear();
                    if (buffer.length > inputBuffers[inputBufferIndex].remaining()) {
                        continue;
                    }
                    int bufferSize = (mChannels == 1) ? (buffer.length / 2) : buffer.length;
                    if (mDecodedBytes.remaining() < bufferSize) {
                        for (int i = mDecodedBytes.remaining(); i < bufferSize; i++) {
                            buffer[i] = 0;  // pad with extra 0s to make a full frame.
                        }
                        mDecodedBytes.get(buffer, 0, mDecodedBytes.remaining());
                    } else {
                        mDecodedBytes.get(buffer, 0, bufferSize);
                    }
                    if (mChannels == 1) {
                        for (int i = bufferSize - 1; i >= 1; i -= 2) {
                            buffer[2 * i + 1] = buffer[i];
                            buffer[2 * i] = buffer[i - 1];
                            buffer[2 * i - 1] = buffer[2 * i + 1];
                            buffer[2 * i - 2] = buffer[2 * i];
                        }
                    }
                    num_samples_left -= frame_size;
                    inputBuffers[inputBufferIndex].put(buffer);
                    presentation_time = (long) (((num_frames++) * frame_size * 1e6) / mSampleRate);
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, buffer.length, presentation_time, 0);
                }
            }
            int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
            if (outputBufferIndex >= 0 && info.size > 0 && info.presentationTimeUs >= 0) {
                if (num_out_frames < frame_sizes.length) {
                    frame_sizes[num_out_frames++] = info.size;
                }
                if (encodedSamplesSize < info.size) {
                    encodedSamplesSize = info.size;
                    encodedSamples = new byte[encodedSamplesSize];
                }
                outputBuffers[outputBufferIndex].get(encodedSamples, 0, info.size);
                outputBuffers[outputBufferIndex].clear();
                codec.releaseOutputBuffer(outputBufferIndex, false);
                if (encodedBytes.remaining() < info.size) {  // Hopefully this should not happen.
                    estimatedEncodedSize = (int) (estimatedEncodedSize * 1.2);  // Add 20%.
                    ByteBuffer newEncodedBytes = ByteBuffer.allocate(estimatedEncodedSize);
                    int position = encodedBytes.position();
                    encodedBytes.rewind();
                    newEncodedBytes.put(encodedBytes);
                    encodedBytes = newEncodedBytes;
                    encodedBytes.position(position);
                }
                encodedBytes.put(encodedSamples, 0, info.size);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = codec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Subsequent data will conform to new format.
                // We could check that codec.getOutputFormat(), which is the new output format,
                // is what we expect.
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                // We got all the encoded data from the encoder.
                break;
            }
        }
        int encoded_size = encodedBytes.position();
        encodedBytes.rewind();
        codec.stop();
        codec.release();
        codec = null;

        // Write the encoded stream to the file, 4kB at a time.
        buffer = new byte[4096];
        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(
                    MP4Header.getMP4Header(mSampleRate, numChannels, frame_sizes, bitrate));
            while (encoded_size - encodedBytes.position() > buffer.length) {
                encodedBytes.get(buffer);
                outputStream.write(buffer);
            }
            int remaining = encoded_size - encodedBytes.position();
            if (remaining > 0) {
                encodedBytes.get(buffer, 0, remaining);
                outputStream.write(buffer, 0, remaining);
            }
            outputStream.close();
        } catch (IOException e) {
            Log.e("Ringdroid", "Failed to create the .m4a file.");
        }
    }

}
