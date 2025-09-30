package com.example.videoconverter;

import org.springframework.beans.factory.annotation.Value;
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
// I'm using @RestController because its methods will return data (JSON) back to the frontend.
@RestController
@RequestMapping("/youtube") // All requests to this controller will start with "/youtube".
public class YoutubeController {

    // I need access to my ConversionService to handle the actual file conversion.
    private final ConversionService conversionService;
    // I also need the ProgressService to track the job status.
    private final ProgressService progressService;

    // This pulls the download directory path from my application.properties file.
    @Value("${app.download-dir}")
    private String downloadDir;

    // This is the path where I'll expect cookies.txt to live.
    @Value("${app.cookies-file:}")
    private String cookiesFile;

    // I'm using constructor injection, which is Spring's recommended way to manage dependencies.
    public YoutubeController(ConversionService conversionService, ProgressService progressService) {
        this.conversionService = conversionService;
        this.progressService = progressService;
    }

    // This method handles the POST request to start a new YouTube conversion.
    @PostMapping("/convert")
    public Map<String, String> convertYoutube(
            @RequestParam("url") String youtubeUrl,
            @RequestParam("format") String format) throws Exception {

        // First, I create a unique ID for this new job.
        String jobId = UUID.randomUUID().toString();
        progressService.createJob(jobId);

        // Before downloading, I run a quick command to get the video's real title.
        String videoTitle = fetchYoutubeTitle(youtubeUrl);
        // I store this clean title in the ProgressService so I can use it for the final download name.
        progressService.setJobFileName(jobId, videoTitle);

        // I ensure the directory for temporary downloads exists.
        Path downloadPath = Path.of(downloadDir);
        if (!Files.exists(downloadPath)) {
            Files.createDirectories(downloadPath);
        }

        // I generate a unique filename for the temporary downloaded file from yt-dlp.
        String uniqueId = UUID.randomUUID().toString();
        String outputTemplate = downloadPath.resolve("ytvideo-" + uniqueId + ".%(ext)s").toString();

        // I run the main yt-dlp command to download the video.
        int exitCode = runYtDlp(youtubeUrl, outputTemplate);
        if (exitCode != 0) {
            // If the download fails, I throw an exception.
            throw new RuntimeException("yt-dlp failed with exit code " + exitCode);
        }

        // After downloading, I find the file that yt-dlp created.
        File downloadedFile = findDownloadedFile(downloadPath, uniqueId);
        // Then I start the conversion process, which runs in the background through @Async.
        conversionService.convertFile(downloadedFile, format, jobId);

        // I immediately return the jobId to the frontend so it can start polling for progress.
        return Map.of("jobId", jobId);
    }

    // This helper method's only job is to get the YouTube video's title.
    private String fetchYoutubeTitle(String youtubeUrl) {
        try {
            ProcessBuilder pb;
            if (cookiesFile != null && !cookiesFile.isEmpty() && new File(cookiesFile).exists()) {
                // If cookies.txt exists, I include it in the yt-dlp command.
                pb = new ProcessBuilder("yt-dlp", "--cookies", cookiesFile, "--get-title", youtubeUrl);
            } else {
                // Otherwise, I run it without cookies.
                pb = new ProcessBuilder("yt-dlp", "--get-title", youtubeUrl);
            }

            Process process = pb.start();

            // I need to read the output of the command to capture the title.
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String title = reader.readLine();
                process.waitFor(); // I wait for the command to finish.

                if (title != null && !title.isEmpty()) {
                    // I clean the title to remove any characters that aren't allowed in a filename.
                    return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(); // If something goes wrong, I print the error.
        }
        // If I can't get the title, I just return a generic name.
        return "youtube-video";
    }

    // This helper method runs the main yt-dlp command to download the video and audio.
    private int runYtDlp(String youtubeUrl, String outputTemplate) throws IOException, InterruptedException {
        ProcessBuilder pb;

        if (cookiesFile != null && !cookiesFile.isEmpty() && new File(cookiesFile).exists()) {
            // If cookies.txt exists, I include it in the yt-dlp command.
            pb = new ProcessBuilder(
                    "yt-dlp",
                    "--cookies", cookiesFile,
                    "--no-playlist",         // I only want to download a single video, not a whole playlist.
                    "-f", "bv*+ba/b",        // This flag tells it to get the best available video and audio streams.
                    "-o", outputTemplate,    // This is the filename pattern for the downloaded file.
                    youtubeUrl
            );
        } else {
            // Otherwise, I run it without cookies.
            pb = new ProcessBuilder(
                    "yt-dlp",
                    "--no-playlist",
                    "-f", "bv*+ba/b",
                    "-o", outputTemplate,
                    youtubeUrl
            );
        }

        pb.inheritIO(); // This pipes the command's output to my console, which makes it easier for debugging.
        Process process = pb.start();
        return process.waitFor(); // I wait for the download to complete and return its exit code.
    }

    // This helper method finds the file that yt-dlp downloaded, since I don't know the exact extension beforehand.
    private File findDownloadedFile(Path downloadPath, String uniqueId) throws IOException {
        // I scan the download directory for a file that starts with the unique ID I generated.
        try (Stream<Path> stream = Files.list(downloadPath)) {
            return stream
                    .filter(f -> f.getFileName().toString().startsWith("ytvideo-" + uniqueId))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Downloaded file not found"))
                    .toFile();
        }
    }
}