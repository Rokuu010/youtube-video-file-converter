package com.example.videoconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// This is the main entry point for my video converter application.
// When I run the project, this class is what starts everything up.
@SpringBootApplication
@EnableAsync // I added this annotation to allow methods to run in the background.
// It's essential for the real-time progress bar, as it lets the
// conversion happen on a separate thread without freezing the UI.
public class VideoConverterApplication {

    // The main method that launches the entire Spring Boot application.
    public static void main(String[] args) {
        SpringApplication.run(VideoConverterApplication.class, args);
    }
}

