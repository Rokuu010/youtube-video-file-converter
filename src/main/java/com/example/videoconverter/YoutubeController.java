package com.example.videoconverter;

import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

// This is my controller for handling all YouTube-related requests.
@RestController
@RequestMapping("/youtube") // All requests to this controller will start with "/youtube".
public class YoutubeController {

    private final ConversionService conversionService;
    private final ProgressService progressService;

    public YoutubeController(ConversionService conversionService, ProgressService progressService) {
        this.conversionService = conversionService;
        this.progressService = progressService;
    }

    // This method handles the POST request to start a new YouTube conversion.
    @PostMapping("/convert")
    public Map<String, String> convertYoutube(
            @RequestParam("url") String youtubeUrl,
            @RequestParam("format") String format) throws Exception {

        String jobId = UUID.randomUUID().toString();
        progressService.createJob(jobId);

        String videoTitle = fetchYoutubeTitle(youtubeUrl);
        progressService.setJobFileName(jobId, videoTitle);

        // This is the temporary directory.
        Path downloadPath = Path.of(System.getProperty("java.io.tmpdir"));
        if (!Files.exists(downloadPath)) {
            Files.createDirectories(downloadPath);
        }

        String uniqueId = UUID.randomUUID().toString();
        String outputTemplate = downloadPath.resolve("ytvideo-" + uniqueId + ".%(ext)s").toString();

        int exitCode = runYtDlp(youtubeUrl, outputTemplate);
        if (exitCode != 0) {
            throw new RuntimeException("yt-dlp failed with exit code " + exitCode);
        }

        File downloadedFile = findDownloadedFile(downloadPath, uniqueId);
        conversionService.convertFile(downloadedFile, format, jobId);

        return Map.of("jobId", jobId);
    }

    private String fetchYoutubeTitle(String youtubeUrl) {
        try {
            // A command to get the video title.
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--get-title", youtubeUrl);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String title = reader.readLine();
                process.waitFor();

                if (title != null && !title.isEmpty()) {
                    // Clean the title to remove any characters that aren't allowed in a filename.
                    return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // If the title can't be fetched, return a generic name.
        return "youtube-video";
    }

    private int runYtDlp(String youtubeUrl, String outputTemplate) throws IOException, InterruptedException {
        // The standard yt-dlp command for downloading video and audio.
        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "--no-playlist",
                "-f", "bv*+ba/b",
                "-o", outputTemplate,
                youtubeUrl
        );

        pb.inheritIO(); // The command's output to the console for easier debugging.
        Process process = pb.start();
        return process.waitFor();
    }

    private File findDownloadedFile(Path downloadPath, String uniqueId) throws IOException {
        try (Stream<Path> stream = Files.list(downloadPath)) {
            return stream
                    .filter(f -> f.getFileName().toString().startsWith("ytvideo-" + uniqueId))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Downloaded file not found for ID: " + uniqueId))
                    .toFile();
        }
    }
}