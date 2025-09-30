package com.example.videoconverter;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

// This is the core of my application, where the actual video and audio conversion happens.
// I've marked it as a @Service so Spring Boot can manage it.
@Service
public class ConversionService {

    // I need the ProgressService to report back on how the conversion is going.
    private final ProgressService progressService;

    // This is the constructor. Spring automatically provides the ProgressService instance for me here.
    public ConversionService(ProgressService progressService) {
        this.progressService = progressService;
    }

    // I've marked this method as @Async so it runs on a background thread.
    // This is the key to preventing the UI from freezing during a long conversion.
    @Async
    public void convertFile(File input, String format, String jobId) {
        File output = null;
        try {
            // This creates a unique temporary file for the output to avoid naming conflicts.
            output = File.createTempFile("output-" + UUID.randomUUID(), "." + format);

            // FFmpegFrameGrabber from the javacv library is what I use to read the input file frame by frame.
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input)) {
                grabber.start();

                // I switched to time-based progress because it's more reliable than counting frames.
                // getLengthInTime() gives me the total duration in microseconds.
                long totalDuration = grabber.getLengthInTime();
                int lastReportedProgress = 0;

                // Here, I check if the target format is audio-only.
                boolean audioOnly = format.equalsIgnoreCase("mp3")
                        || format.equalsIgnoreCase("wav")
                        || format.equalsIgnoreCase("ogg");

                // I get the video/audio properties from the original file to set up the output correctly.
                int width = audioOnly ? 0 : grabber.getImageWidth();
                int height = audioOnly ? 0 : grabber.getImageHeight();
                int audioChannels = grabber.getAudioChannels();

                // FFmpegFrameRecorder is what writes the new, converted file.
                try (FFmpegFrameRecorder recorder =
                             new FFmpegFrameRecorder(output, width, height, audioChannels)) {

                    // I set the output format and the correct audio/video codecs based on the user's choice.
                    recorder.setFormat(format);
                    if (audioOnly) {
                        if (format.equalsIgnoreCase("wav")) {
                            recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
                        } else if (format.equalsIgnoreCase("mp3")) {
                            recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
                        } else if (format.equalsIgnoreCase("ogg")) {
                            recorder.setAudioCodec(avcodec.AV_CODEC_ID_VORBIS);
                        }
                    } else {
                        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                        recorder.setFrameRate(grabber.getFrameRate());
                        recorder.setVideoBitrate(grabber.getVideoBitrate());
                    }
                    recorder.setSampleRate(grabber.getSampleRate());
                    recorder.start();

                    // This is the main loop where I read a frame, write a frame, and repeat.
                    Frame frame;
                    while ((frame = grabber.grab()) != null) {
                        if (audioOnly) {
                            if (frame.samples != null) {
                                recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples);
                            }
                        } else {
                            recorder.record(frame);
                        }

                        // This is my progress reporting logic.
                        if (totalDuration > 0) {
                            long currentTimestamp = grabber.getTimestamp();
                            int progress = (int) (((double) currentTimestamp / totalDuration) * 100);

                            // I only send an update if the percentage has actually changed, to be more efficient.
                            if (progress > lastReportedProgress) {
                                lastReportedProgress = progress;
                                progressService.setProgress(jobId, Math.min(progress, 99)); // Cap at 99 until it's truly done.
                            }
                        }
                    }
                }
            }
            // Once the loop is finished, I tell the ProgressService that the job is 100% complete.
            progressService.setJobCompleted(jobId, output.getAbsolutePath());

        } catch (Exception e) {
            // If anything goes wrong, I print the error and report a failure status (-1) to the frontend.
            e.printStackTrace();
            progressService.setProgress(jobId, -1);
            if (output != null) {
                output.delete();
            }
        } finally {
            // This 'finally' block ensures that the temporary input file is always deleted,
            // even if the conversion fails, which keeps my server clean.
            try {
                if (input != null) {
                    Files.deleteIfExists(input.toPath());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}