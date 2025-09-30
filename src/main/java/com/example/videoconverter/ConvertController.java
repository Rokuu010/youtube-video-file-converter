package com.example.videoconverter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// This is my controller for handling direct file uploads.
// I'm using @RestController because its methods are for my API and return data, not HTML pages.
@RestController
public class ConvertController {

    // I created this list to make sure users can only convert to formats I support.
    // It's a simple security and validation measure.
    private static final List<String> ALLOWED_FORMATS = List.of("mp4", "mp3", "ogg", "wav");

    private final ConversionService conversionService;
    private final ProgressService progressService;

    // Using constructor injection here. Spring automatically provides the services I need.
    public ConvertController(ConversionService conversionService, ProgressService progressService) {
        this.conversionService = conversionService;
        this.progressService = progressService;
    }

    // This method handles the POST request when a user uploads a file using the form.
    @PostMapping("/convert")
    public ResponseEntity<Map<String, String>> convertVideo(
            // @RequestParam("file") catches the uploaded file. The name "file" must match the name in my HTML form.
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format) throws IOException {

        // First, I do some basic validation.
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded."));
        }

        format = format.toLowerCase();
        if (!ALLOWED_FORMATS.contains(format)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported format: " + format));
        }

        // I get the original name of the file the user uploaded.
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";

        // I'm cleaning up the filename here by removing the extension (e.g., ".mp3")
        // This gives me a clean base name to use for the final downloaded file.
        String baseFileName = originalName.contains(".")
                ? originalName.substring(0, originalName.lastIndexOf('.'))
                : originalName;

        // I save the uploaded file to a temporary location on the server so my ConversionService can access it.
        File tempInputFile = File.createTempFile("input-" + UUID.randomUUID(), "-" + originalName);
        file.transferTo(tempInputFile);

        // I generate a unique ID for this specific conversion. This is the "ticket number" for the frontend.
        String jobId = UUID.randomUUID().toString();
        progressService.createJob(jobId);

        // I store the clean filename in the ProgressService so it's ready for the final download.
        progressService.setJobFileName(jobId, baseFileName);

        // Here I kick off the actual conversion. Because it's an @Async method, it runs in the background.
        conversionService.convertFile(tempInputFile, format, jobId);

        // I immediately send the jobId back to the browser. The frontend can now start checking for progress.
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }
}
