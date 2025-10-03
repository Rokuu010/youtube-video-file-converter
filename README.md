# My Video Converter Web App

A personal project I built to create a simple, clean tool for converting video and audio files, made mainly with Java and Spring Boot.

<img width="2878" height="1742" alt="Screenshot converter" src="https://github.com/user-attachments/assets/bb1aac56-f578-401c-b81e-9e12177741e3" />

---

## 📖 About The Project

I wanted to create a project for my **portfolio** that would showcase my **Java skills** and demonstrate my ability to build a small full-stack web application. The idea for a media file converter was perfect, as it requires handling file uploads, managing backend processing logic, and integrating third-party tools.

This project started out as a simple command-line tool but I decided to challenge myself and evolve it into a **full-fledged web application**. This journey taught me a lot about building a backend with **Spring Boot**, handling web requests and **file uploads**, and integrating powerful **third-party tools** like FFmpeg via a Java wrapper.

The end result is a clean, simple, single-page web app that focuses on performing core conversions efficiently.

---

## ✨ Features

* **Simple & Clean UI:** A straightforward interface for uploading files.
* **Core Format Support:** Convert files to the most popular and essential formats:
    * **Audio:** **MP3**, **WAV**
    * **Video:** **MP4**, **OGG**
* **Direct Download:** As soon as the conversion is done, the file is sent straight back to your browser for immediate download.

---

## 🛠️ Built With

I used these technologies to bring this project to life:

* **Backend:** Java 21, Spring Boot, Maven
* **Media Processing:** **JavaCV** (as a Java wrapper for **FFmpeg**)
* **Frontend:** HTML5 & CSS3

---

## 🚀 Getting Started

If you want to get a copy of this running on your own machine, here’s how.

### Prerequisites

You'll need a few things installed on your system first:

* **Java Development Kit (JDK) 17 or later.**
* **Apache Maven.**
* **FFmpeg:** This is the core engine that does all the converting. It needs to be installed and available on your system's PATH.
    * *macOS (Homebrew):* `brew install ffmpeg`
    * *Windows (Scoop/Chocolatey):* `scoop install ffmpeg` or `choco install ffmpeg`
    * *Linux (apt):* `sudo apt update && sudo apt install ffmpeg`

### Installation & Running

1.  **Clone the repo:**
    ```sh
    git clone [https://github.com/your-username/video-converter.git](https://github.com/your-username/video-converter.git)
    ```
2.  **Jump into the project directory:**
    ```sh
    cd video-converter
    ```
3.  **Run the app with Maven:**
    * On macOS/Linux:
        ```sh
        ./mvnw spring-boot:run
        ```
    * On Windows:
        ```sh
        mvnw spring-boot:run
        ```
The app will start up, and you'll see a message in the console like `Tomcat started on port(s): 8080`.

---

## 💻 How to Use It

1. Open your browser and head to `http://localhost:8080`.
2. Upload a media file.
3. Pick the format you want to convert to.
4. Hit the "Convert" button.
5. Your browser will start downloading the new file. Simple as that!
