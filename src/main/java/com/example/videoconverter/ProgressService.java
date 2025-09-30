package com.example.videoconverter;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// This service is the "brain" of my progress tracking system.
// It's a central place to keep track of every conversion job that's currently running.
@Service
public class ProgressService {

    // I created this small inner class to keep all the information for a single job neatly organized.
    // It holds the progress percentage, the path to the final file, and the desired filename for the download.
    public static class JobStatus {
        public int progress = 0;
        public String resultPath;
        public String finalFileName = "converted-file"; // I set a default name just in case.
    }

    // I'm using a ConcurrentHashMap here because my conversions run on background threads.
    // This type of map is thread-safe, which prevents errors if multiple jobs update at the same time.
    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();

    // When a new conversion starts, I call this method to create a new "ticket" for it in my jobs map.
    public void createJob(String jobId) {
        jobs.put(jobId, new JobStatus());
    }

    // My ConversionService calls this method repeatedly to update the progress percentage.
    public void setProgress(String jobId, int progress) {
        // 'computeIfPresent' is a safe way to update an entry in the map.
        jobs.computeIfPresent(jobId, (id, status) -> {
            status.progress = progress;
            return status;
        });
    }

    // My controllers call this method to store the correct, user-friendly filename for a job.
    public void setJobFileName(String jobId, String finalFileName) {
        jobs.computeIfPresent(jobId, (id, status) -> {
            status.finalFileName = finalFileName;
            return status;
        });
    }

    // When a conversion is 100% done, I call this to mark it as complete and store the path to the temporary file.
    public void setJobCompleted(String jobId, String resultPath) {
        jobs.computeIfPresent(jobId, (id, status) -> {
            status.progress = 100;
            status.resultPath = resultPath;
            return status;
        });
    }

    // My ProgressController uses this to get all the current information about a job.
    public JobStatus getJobStatus(String jobId) {
        return jobs.get(jobId);
    }

    // Once a file has been downloaded, I call this to remove the job from the map to keep things clean.
    public void removeJob(String jobId) {
        jobs.remove(jobId);
    }
}