// This is just a quick check to make sure my script file is being loaded by the browser.
console.log("app.js script loaded!");

// I'm wrapping all my code in this event listener.
// It makes sure the HTML page is fully loaded before my JavaScript tries to run.
document.addEventListener('DOMContentLoaded', () => {
    console.log("DOM fully loaded and parsed.");

    // Here, I'm grabbing all the interactive parts of my HTML page and storing them in variables.
    // This makes it easier for me to reference them later in the code.
    const showUploadBtn = document.getElementById('show-upload-btn');
    const showYoutubeBtn = document.getElementById('show-youtube-btn');
    const uploadSection = document.getElementById('upload-section');
    const youtubeSection = document.getElementById('youtube-section');
    const youtubeForm = document.getElementById('youtube-form');
    const fileUploadForm = document.getElementById('file-upload-form');
    const progressContainer = document.getElementById('progress-container');
    const progressBar = progressContainer.querySelector('.progress-bar');
    const resultAlert = document.getElementById('result-alert');
    const dropArea = document.getElementById('drop-area');
    const fileInput = document.getElementById('fileInput');
    const fileNameDisplay = document.getElementById('file-name');

    // This variable will hold my timer for checking the conversion progress.
    let pollInterval;

    // This section handles the logic for switching between the "UPLOAD" and "YOUTUBE" views.
    if (showUploadBtn && showYoutubeBtn && uploadSection && youtubeSection) {
        showUploadBtn.addEventListener('click', () => {
            showUploadBtn.classList.add('active');
            showYoutubeBtn.classList.remove('active');
            uploadSection.classList.remove('d-none');
            youtubeSection.classList.add('d-none');
        });

        showYoutubeBtn.addEventListener('click', () => {
            showYoutubeBtn.classList.add('active');
            showUploadBtn.classList.remove('active');
            youtubeSection.classList.remove('d-none');
            uploadSection.classList.add('d-none');
        });
    }

    // This is all the logic for my drag-and-drop area.
    if (dropArea && fileInput && fileNameDisplay) {
        // If I click the area, it should open the file selection dialog.
        dropArea.addEventListener('click', () => fileInput.click());

        // These add a 'highlight' class when I drag a file over the area for a visual effect.
        ['dragenter', 'dragover'].forEach(eventName => {
            dropArea.addEventListener(eventName, e => { e.preventDefault(); e.stopPropagation(); dropArea.classList.add('highlight'); }, false);
        });
        // These remove the highlight when the file is dropped or dragged away.
        ['dragleave', 'drop'].forEach(eventName => {
            dropArea.addEventListener(eventName, e => { e.preventDefault(); e.stopPropagation(); dropArea.classList.remove('highlight'); }, false);
        });

        // This handles the file when it's actually dropped onto the area.
        dropArea.addEventListener('drop', e => {
            fileInput.files = e.dataTransfer.files;
            updateFileName();
        }, false);
        // This handles the case where I select a file by clicking.
        fileInput.addEventListener('change', updateFileName);

        // A simple helper function to display the name of the selected file.
        function updateFileName() {
            if (fileInput.files.length > 0) {
                fileNameDisplay.textContent = fileInput.files[0].name;
            } else {
                fileNameDisplay.textContent = 'No file selected';
            }
        }
    }

    // This is what happens when I click the "CONVERT FILE" button.
    fileUploadForm.addEventListener('submit', (e) => {
        e.preventDefault(); // This stops the page from reloading.
        if (fileInput.files.length === 0) {
            showAlert('Please select a file to upload.', 'warning');
            return;
        }
        const formData = new FormData(fileUploadForm); // Package the form data.
        startConversionJob('/convert', { method: 'POST', body: formData });
    });

    // This is what happens when I click the "CONVERT YOUTUBE" button.
    youtubeForm.addEventListener('submit', (e) => {
        e.preventDefault(); // Stop the page from reloading.
        const url = document.getElementById('youtubeUrl').value;
        const format = document.getElementById('youtubeFormat').value;
        // I build the URL with the user's input to send to my backend.
        const fetchUrl = `/youtube/convert?url=${encodeURIComponent(url)}&format=${format}`;
        startConversionJob(fetchUrl, { method: 'POST' });
    });

    // This function starts the conversion job on my backend.
    async function startConversionJob(url, options) {
        // First, I reset the UI to a loading state.
        progressContainer.style.display = 'block';
        progressBar.style.width = '0%';
        progressBar.classList.remove('bg-danger');
        resultAlert.style.display = 'none';
        if(pollInterval) clearInterval(pollInterval); // I clear any old timers just in case.

        try {
            // I send the request to my backend to start the job.
            const response = await fetch(url, options);
            if (!response.ok) throw new Error('Failed to start the conversion job.');

            // My backend should immediately send back a unique job ID.
            const data = await response.json();
            if (data.jobId) {
                // If I get a job ID, I start polling for progress.
                pollProgress(data.jobId);
            } else {
                throw new Error('Server did not return a job ID.');
            }
        } catch (error) {
            console.error('Error starting conversion:', error);
            showAlert('Error: Could not start conversion.', 'danger');
            progressContainer.style.display = 'none';
        }
    }

    // This function repeatedly asks my server for the status of a job.
    function pollProgress(jobId) {
        // I'm using setInterval to run this code every 2 seconds.
        pollInterval = setInterval(async () => {
            try {
                // I ask my server for the progress of the specific job.
                // I added a timestamp to the URL to prevent the browser from caching the result.
                const response = await fetch(`/progress/${jobId}?_=${new Date().getTime()}`);
                if (response.status === 404) {
                    clearInterval(pollInterval);
                    return;
                }
                if (!response.ok) throw new Error('Progress check failed.');

                const data = await response.json();
                const progress = data.progress;

                if (progress < 0) {
                    // Progress of -1 means my backend had an error.
                    clearInterval(pollInterval);
                    showAlert('Conversion failed on the server.', 'danger');
                    progressBar.classList.add('bg-danger');
                    progressBar.style.width = '100%';
                } else if (progress >= 100) {
                    // The job is done!
                    clearInterval(pollInterval); // Stop the timer.
                    progressBar.style.width = '100%';
                    showAlert('Conversion complete!', 'success');

                    // I trigger the download by redirecting the browser.
                    window.location.href = `/download/${jobId}`;

                    // After 5 seconds, I hide the progress bar and message.
                    setTimeout(() => {
                        progressContainer.style.display = 'none';
                        resultAlert.style.display = 'none';
                    }, 5000);
                } else {
                    // If the job is still running, I update the progress bar's width.
                    progressBar.style.width = `${progress}%`;
                }
            } catch (error) {
                console.error('Polling error:', error);
                clearInterval(pollInterval);
                showAlert('Error checking conversion status.', 'danger');
                progressContainer.style.display = 'none';
            }
        }, 2000);
    }

    // A simple function I wrote to show messages to the user.
    function showAlert(message, type) {
        if (resultAlert) {
            resultAlert.className = `alert alert-${type} mt-3`;
            resultAlert.textContent = message;
            resultAlert.style.display = 'block';
        }
    }
});

