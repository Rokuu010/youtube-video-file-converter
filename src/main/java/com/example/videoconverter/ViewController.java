package com.example.videoconverter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// I created this controller specifically to handle serving my HTML page.
// I'm using @Controller here (instead of @RestController) because its job is to return a view template, not raw data.
@Controller
public class ViewController {

    // This method handles GET requests for the root URL of my site.
    @GetMapping("/")
    public String index() {
        // This tells Spring Boot to find and render the HTML file named "index.html"
        // from my folder.
        return "index";
    }
}