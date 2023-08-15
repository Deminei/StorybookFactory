package com.example.flub.service;

import com.example.flub.model.Story;
import com.example.flub.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.net.URL;

@Service
public class OpenAIService {

    private final StoryRepository storyRepository;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String modelVersion;

    @Autowired
    public OpenAIService(StoryRepository storyRepository,
                         @Value("${openai.api.baseurl}") String apiBaseUrl,
                         @Value("${openai.api.key}") String apiKey,
                         @Value("${openai.model.version}") String modelVersion) {
        this.storyRepository = storyRepository;
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;
        this.modelVersion = modelVersion;
    }

    public String getInitialPrompt() {

        String initialPrompt = "Guide the user in an interactive story from their perspective. Refer to the user as 'The hero' or 'you'. Start with: 'You are the hero of a thrilling adventure...'. Offer choices as questions. Limit responses to 100 words.";
        return initialPrompt;
    }

    public String getInitialStory() {
        // Call the chatGPT method with the initial prompt to get the initial story
        String initialPrompt = getInitialPrompt();

        // Initialize conversation history as an empty string for the initial story
        String conversationHistory = "";

        // Build the full prompt by combining the conversation history and initial prompt
        String fullPrompt = conversationHistory + " " + initialPrompt;

        String initialStory = chatGPT(conversationHistory, fullPrompt);

        // Save the initial story as a Story entity in the database
        Story story = new Story();
        story.setPrompt(fullPrompt); // Save the entire conversation history and initial prompt as the prompt
        story.setStoryText(initialStory);
        storyRepository.save(story);

        return initialStory;
    }


    public String continueStory(String storyContext, String userChoice) {
        // Provide the continue prompt for the AI response
        String continuePrompt = "Continue the story from the user's perspective using the what's been established and implement user's choice: " + userChoice + ". Offer new questions for the user's next decision. Limit responses to 100 words and exclude the previous story.";

        // Combine the storyContext and userChoice to form the new conversation history
        String newConversationHistory = buildConversationHistory(storyContext, userChoice);

        // Call the chatGPT method with the new conversation history and continue prompt
        String aiResponse = chatGPT(newConversationHistory, continuePrompt);


        return aiResponse;
    }

    public String generateEnding(String storyContext, String prompt) {

        String finalConverstaionHistory = buildConversationHistory(storyContext, prompt);

        String aiResponse = chatGPT(finalConverstaionHistory, prompt);
        return aiResponse;
    }



    private String buildConversationHistory(String storyContext, String userChoice) {
        // If there is no existing story context, use only the user choice as the conversation history
        if (storyContext == null || storyContext.isEmpty()) {
            return "[{\"role\": \"user\", \"content\": \"" + userChoice + "\"}]";
        }

        // Combine the storyContext and userChoice to form the conversation history
        String conversationHistory = "[{\"role\": \"user\", \"content\": \"" + storyContext + "\"},"
                + "{\"role\": \"assistant\", \"content\": \"" + userChoice + "\"}]";

        return conversationHistory;
    }


    public String chatGPT(String conversationHistory, String fullPrompt) {
        String url = apiBaseUrl + "/chat/completions";

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            String body = "{\"model\": \"" + modelVersion + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + fullPrompt + "\"}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            // Check the response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorLine;
                StringBuffer errorResponse = new StringBuffer();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                throw new RuntimeException("API request failed with response code " + responseCode + ": " + errorResponse.toString());
            }

            // Response from ChatGPT
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            // Process the response to remove line breaks with \n1, \n2, etc.
            String extractedMessage = extractMessageFromJSONResponse(response.toString());
            extractedMessage = extractedMessage.replaceAll("\\\\n\\d+", ""); // Remove \n1, \n2, etc.

            // Save the extracted message as a Story entity in the database
            Story story = new Story();
            story.setPrompt(conversationHistory); // Save the entire conversation history as the prompt
            story.setStoryText(extractedMessage);
            storyRepository.save(story);

            return extractedMessage;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static String extractMessageFromJSONResponse(String response) {
        // Parse the JSON response to extract the assistant's reply
        Gson gson = new Gson();
        JsonObject responseObject = gson.fromJson(response, JsonObject.class);
        JsonArray choicesArray = responseObject.getAsJsonArray("choices");
        JsonObject messageObject = choicesArray.get(0).getAsJsonObject().getAsJsonObject("message");

        return messageObject.get("content").getAsString();
    }
}