# StorybookFactory

This project provides an interactive storytelling experience where users can shape the narrative based on their choices. The backend communicates with the OpenAI API to dynamically generate story segments.

## Prerequisites

- Docker installed on your machine.
- Two unique OpenAI API keys.

## Configuration

Before you can run this project, you need to provide your OpenAI API keys.

1. Enter one key in the configuration file application.yml:
![Screenshot](images/instructionsReadme.png)

2. Enter the other key in the StoryController in the generateImage method:
![Screenshot](images/instructionsReadme2.png)

3. Build the Docker image:

```bash
docker build -t my-app-image .
```

4. Run the Docker container:

```bash
docker run -p 8080:8080 my-app-image
```
or run the image directly from Docker desktop.

Your application should now be running and accessible at `http://localhost:8080`.

## Contributors

Kevin Hoang

Sher Yang

Alexa Dickenson

Cierra Hampton
