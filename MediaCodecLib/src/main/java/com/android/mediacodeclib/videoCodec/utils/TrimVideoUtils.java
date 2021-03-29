package com.android.mediacodeclib.videoCodec.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.mediacodeclib.videoCodec.interfaces.OnTrimVideoListener;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class TrimVideoUtils {
    private static final String TAG = TrimVideoUtils.class.getSimpleName();

    public static void startTrim(@NonNull File file, @NonNull String destination, long startMs, long endMs, @NonNull OnTrimVideoListener callback) throws IOException {
        final String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        final String filename = "MP4_" + timestamp + ".mp4";
        final String filePath = destination + filename;

        File currentFile = new File(filePath);
        currentFile.getParentFile().mkdirs();
        Log.d(TAG, "Generated file path" + filePath);
        generateVideoUsingMp4Parser(file, currentFile, startMs, endMs, callback);
    }

    private static void generateVideoUsingMp4Parser(@NonNull File file, @NonNull File currentFile, long startMs, long endMs, @NonNull OnTrimVideoListener callback) throws IOException {

        Movie movie = MovieCreator.build(new FileDataSourceViaHeapImpl(file.getAbsolutePath()));

        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());

        double startTime = startMs / 1000;
        double endTime = endMs / 1000;

        boolean timeCorrected = false;

        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    throw new RuntimeException("The startTime has already been corrected by another track with syncsample. Not supported");
                }
                startTime = correctTimetoSyncSample(track, startTime, false);
                endTime = correctTimetoSyncSample(track, endTime, true);
                timeCorrected = true;
            }
        }

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = -1;
            long startSample = -1;
            long endSample = -1;

            for (int i = 0; i < track.getSampleDurations().length; i++) {
                long delta = track.getSampleDurations()[i];

                if (currentTime > lastTime && currentTime <= startTime) {
                    startSample = currentSample;
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    endSample = currentSample;
                }
                lastTime = currentTime;
                currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
            movie.addTrack(new AppendTrack(new CroppedTrack(track, startSample, endSample)));
        }

        currentFile.getParentFile().mkdirs();

        if (currentFile.exists() == false) {
            currentFile.createNewFile();
        }

        Container out = new DefaultMp4Builder().build(movie);

        FileOutputStream fileOutputStream = new FileOutputStream(currentFile);
        FileChannel channel = fileOutputStream.getChannel();
        out.writeContainer(channel);

        channel.close();
        fileOutputStream.close();
        if (callback != null) {
            callback.getResult(Uri.parse(currentFile.toString()));
        }
    }

    private static double correctTimetoSyncSample(Track track, double cutHere, boolean flag) {
        double[] timeofSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];
            int position = Arrays.binarySearch(track.getSyncSamples(), currentSample + 1);
            if (position >= 0) {
                timeofSyncSamples[position] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;
        }

        double previous = 0;
        for (double sample : timeofSyncSamples) {
            if (sample > cutHere) {
                return flag ? sample : previous;
            }
            previous = sample;
        }
        return timeofSyncSamples[timeofSyncSamples.length - 1];
    }

    public static String stringForTime(int timeInMs) {
        int totalSeconds = timeInMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        Formatter formatter = new Formatter();
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
}
