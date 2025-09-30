package com.example.videoconverter;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// This service is responsible for tracking the status of all active conversion jobs.
// It's a central place to store progress, final file paths, and filenames.
@Service
public class ProgressService {

    // I'm using a ConcurrentHashMap because it's thread-safe, which is essential since
    // conversions run on background threads (@Async) while the main thread might be checking the status.
    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();

    // This creates a new entry in my map to start tracking a new job.
    public void createJob(String jobId) {
        jobs.put(jobId, new JobStatus());
    }

    // This updates the progress percentage for a given job.
    public void setProgress(String jobId, int progress) {
        JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.setProgress(progress);
        }
    }

    // This marks a job as complete and stores the path to the final converted file.
    public void setJobCompleted(String jobId, String filePath) {
        JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.setProgress(100);
            status.setFilePath(filePath);
        }
    }

    // This is the method the YoutubeController uses to save the video's title.
    public void setJobFileName(String jobId, String fileName) {
        JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.setFileName(fileName);
        }
    }

    // This is the method that your ConversionService needs to get the saved title.
    public String getJobFileName(String jobId) {
        JobStatus status = jobs.get(jobId);
        return (status != null) ? status.getFileName() : null;
    }

    // This method is used by the frontend to poll for the current status of a job.
    public JobStatus getJobStatus(String jobId) {
        return jobs.get(jobId);
    }

    // This is the new method that your ProgressController needs.
    // Once a file has been downloaded, I call this to remove the job from the map to keep things clean.
    public void removeJob(String jobId) {
        jobs.remove(jobId);
    }

    // This is a simple data-holder class to keep all the information
    // about a single job organised in one place.
    public static class JobStatus {
        private int progress = 0;
        private String filePath;
        private String fileName;

        // Standard getters and setters for the job properties.
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
    }
}