package com.example.videoconverter;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.util.Map;

// This controller handles all the client-side polling for progress and the final download requests.
// I'm using @RestController because it's designed to return data, like the JSON progress updates.
@RestController
public class ProgressController {

    // I need the ProgressService to get the status of the ongoing conversion jobs.
    private final ProgressService progressService;

    // Spring injects the ProgressService for me here.
    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    // This is the endpoint my frontend JavaScript polls every couple of seconds.
    // The {jobId} in the path is a variable that holds the unique ID for the conversion.
    @GetMapping("/progress/{jobId}")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable String jobId) {
        // I fetch the current status of the job using its ID.
        ProgressService.JobStatus status = progressService.getJobStatus(jobId);

        // If the job doesn't exist, I send a 404 Not Found response.
        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        // I return a JSON object with both the progress percentage and the final filename.
        // My JavaScript uses this to update the progress bar.
        // I'm now using the public getter methods to access the private fields.
        return ResponseEntity.ok(Map.of(
                "progress", status.getProgress(),
                "fileName", status.getFileName()
        ));
    }

    // This is the endpoint the browser is redirected to when the conversion is 100% complete.
    @GetMapping("/download/{jobId}")
    public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String jobId) {
        ProgressService.JobStatus status = progressService.getJobStatus(jobId);

        // I do a quick check to make sure the job actually exists and is fully complete before trying to send the file.
        // I'm using the getter methods here as well.
        if (status == null || status.getProgress() < 100 || status.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(status.getFilePath());
        String filePath = status.getFilePath();

        // I get the file extension (like ".mp3") from the temporary file's path.
        String extension = filePath.substring(filePath.lastIndexOf("."));

        // I create the final, filename by combining the stored name (e.g., "My Video") with the extension.
        String finalFileName = status.getFileName() + extension;

        // After getting all the info I need, I remove the job to keep my service clean.
        progressService.removeJob(jobId);

        // I return the file. The "Content-Disposition" header is what tells the browser
        // to treat this as a download and what to name the file.
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + finalFileName + "\"")
                .body(new FileSystemResource(file));
    }
}
