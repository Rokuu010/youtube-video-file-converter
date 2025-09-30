package com.example.videoconverter;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

// This service handles the core video/audio conversion logic.
@Service
public class ConversionService {

    private final ProgressService progressService;

    public ConversionService(ProgressService progressService) {
        this.progressService = progressService;
    }

    // This method runs on a background thread to keep the UI responsive.
    @Async
    public void convertFile(File input, String format, String jobId) {
        File output = null;
        try {
            // This saves the final file to the user's Downloads folder.
            String userHome = System.getProperty("user.home");
            Path downloadDirPath = Path.of(userHome, "Downloads");

            // Ensure the Downloads directory exists.
            if (!Files.exists(downloadDirPath)) {
                Files.createDirectories(downloadDirPath);
            }

            // Get the clean filename from the progress service and create the final path.
            String finalFileName = progressService.getJobFileName(jobId);
            output = downloadDirPath.resolve(finalFileName + "." + format).toFile();

            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input)) {
                grabber.start();

                long totalDuration = grabber.getLengthInTime();
                int lastReportedProgress = 0;

                boolean audioOnly = format.equalsIgnoreCase("mp3")
                        || format.equalsIgnoreCase("wav")
                        || format.equalsIgnoreCase("ogg");

                int width = audioOnly ? 0 : grabber.getImageWidth();
                int height = audioOnly ? 0 : grabber.getImageHeight();
                int audioChannels = grabber.getAudioChannels();

                try (FFmpegFrameRecorder recorder =
                             new FFmpegFrameRecorder(output, width, height, audioChannels)) {

                    recorder.setFormat(format);
                    if (audioOnly) {
                        if ("wav".equalsIgnoreCase(format)) {
                            recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
                        } else if ("mp3".equalsIgnoreCase(format)) {
                            recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
                        } else if ("ogg".equalsIgnoreCase(format)) {
                            recorder.setAudioCodec(avcodec.AV_CODEC_ID_VORBIS);
                        }
                    } else {
                        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                        recorder.setFrameRate(grabber.getFrameRate());
                        recorder.setVideoBitrate(grabber.getVideoBitrate());
                    }
                    recorder.setSampleRate(grabber.getSampleRate());
                    recorder.start();

                    Frame frame;
                    while ((frame = grabber.grab()) != null) {
                        if (audioOnly) {
                            if (frame.samples != null) {
                                recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples);
                            }
                        } else {
                            recorder.record(frame);
                        }

                        if (totalDuration > 0) {
                            long currentTimestamp = grabber.getTimestamp();
                            int progress = (int) (((double) currentTimestamp / totalDuration) * 100);

                            if (progress > lastReportedProgress) {
                                lastReportedProgress = progress;
                                progressService.setProgress(jobId, Math.min(progress, 99));
                            }
                        }
                    }
                }
            }
            // Once finished, mark the job as complete and provide the final path.
            progressService.setJobCompleted(jobId, output.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            progressService.setProgress(jobId, -1); // Mark job as failed.
            if (output != null) {
                output.delete();
            }
        } finally {
            // Delete the temporary input file to keep the system clean.
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

