package com.example.flub.controller;

import com.example.flub.model.Story;
import com.example.flub.repository.StoryRepository;
import com.example.flub.service.OpenAIService;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

@Controller
public class StoryController {

    private final OpenAIService openAIService;
    private final StoryRepository storyRepository;
    private final List<String> generatedStories;

    private List<String> generatedImageUrls; // New list to store the generated image URLs

    public StoryController(OpenAIService openAIService, StoryRepository storyRepository) {
        this.openAIService = openAIService;
        this.storyRepository = storyRepository;
        this.generatedImageUrls = new ArrayList<>();
        this.generatedStories = new ArrayList<>();
    }


    @GetMapping("/story")
    public String getStory(Model model) {
        String initialStory = openAIService.getInitialStory();
        model.addAttribute("story", initialStory);
        generatedStories.add(initialStory);
        generatedImageUrls.add(generateImage(initialStory));

        Long initialStoryId = 1L;

        model.addAttribute("storyId", initialStoryId);
        return "story";
    }


    @PostMapping("/continue-story")
    @ResponseBody
    public ResponseEntity<Map<String, String>> continueStory(@RequestParam("userChoice") String userChoice,
                                                             @RequestParam("storyContext") String storyContext) {
        // Call the continueStory method in OpenAIService to generate the AI response
        String aiResponse = openAIService.continueStory(storyContext, userChoice);
        // To add response to string list KH
        generatedStories.add(aiResponse);
        // Create a JSON response containing the AI response
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("aiResponse", aiResponse);

        // Generate the image URL for the current story segment and add it to the list - KH
        String imageUrl = generateImage(aiResponse);
        generatedImageUrls.add(imageUrl);
        responseMap.put("imageUrls", imageUrl);

        return ResponseEntity.ok(responseMap);
    }

    @PostMapping("/end-story")
    @ResponseBody
    public ResponseEntity<Map<String, String>> endStory(@RequestParam("storyContext") String storyContext) {
        // Construct a prompt for the AI to generate a relevant ending based on the storyContext.
        String prompt = "I want to end this story. Conclude the story from the user's perspective. Avoid questions, choice numbers, or letters. Limit the ending to 100 words.";

        // Call the AI with the constructed prompt to generate an ending.
        String aiResponse = openAIService.generateEnding(storyContext, prompt);
        generatedStories.add(aiResponse);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("aiEndResponse", aiResponse);


        // Generate the image URL for the current story segment and add it to the list - KH
        String imageUrl = generateImage(aiResponse);
        generatedImageUrls.add(imageUrl);
        responseMap.put("imageUrls", imageUrl);

        return ResponseEntity.ok(responseMap);
    }


    private String generateImage(String aiResponse) {
        OpenAiService service = new OpenAiService("");
        String imageUrl;

        String genericPrompt1 = "From this story snippet: ";
        String genericPrompt2 = " , Create a surrealist digital fantasy art";

        CreateImageRequest request = CreateImageRequest.builder()
                .prompt(genericPrompt1 + aiResponse + genericPrompt2)
                .n(1)
                .size("512x512")
                .build();
        imageUrl = service.createImage(request).getData().get(0).getUrl();
        return imageUrl;
    }

    @GetMapping("/export")
    public String getExport(Model model) {

        model.addAttribute("generatedImageUrls", generatedImageUrls); // Add generated image URLs
//        List<Story> stories = storyRepository.findAll(); // Fetch all stories from the repository
        model.addAttribute("stories", generatedStories); // Add the list of stories

        return "export";
    }



    @GetMapping("/export-pdf")
    public void exportImageAndTextToPdf(HttpServletResponse response) throws IOException {
        // Create a PDF document
        PDDocument document = new PDDocument();

        // Define the landscape page size (e.g., A4 landscape)
        PDRectangle landscapePageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        List<String> imageUrls = generatedImageUrls;
        List<String> texts = generatedStories;


        // Loop through the image URLs and add each image and text to the PDF
        for (int i = 0; i < imageUrls.size(); i++) {
            URL url = new URL(imageUrls.get(i));
            InputStream inputStream = url.openStream();
            BufferedImage image = ImageIO.read(inputStream);

            PDPage page = new PDPage(landscapePageSize);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);


            // Create a graphics state object with 10% opacity
            PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
            graphicsState.setNonStrokingAlphaConstant(0.3f); // 30% opacity

            // Save the current graphics state
            contentStream.saveGraphicsState();

            // Set the new graphics state
            contentStream.setGraphicsStateParameters(graphicsState);

            // Draw the image to cover the entire page
            contentStream.drawImage(pdImage, 0, 0, landscapePageSize.getWidth(), landscapePageSize.getHeight()); // This one the opacity is at 10%


            // Restore the previous graphics state (100% opacity)
            contentStream.restoreGraphicsState();

            contentStream.drawImage(pdImage, 320, 20); //Leave original color

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);

            float startX = 20; // x-coordinate to start the text
            float startY = 520; // y-coordinate to start the text
            float maxWidth = 280; // Maximum width for the text


            String[] words = texts.get(i).split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                // Calculate the width of the line with the new word
                float lineWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(line.toString() + " " + word) / 1000 * 12;

                // If the line width exceeds the maximum width, start a new line
                if (lineWidth > maxWidth) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(startX, startY);
                    contentStream.showText(line.toString());
                    contentStream.endText();

                    startY -= 14; // Adjust the y-coordinate for the new line
                    line = new StringBuilder();
                }

                // Append the word to the current line
                line.append(word).append(" ");
            }

            // Draw the last line of text
            contentStream.beginText();
            contentStream.newLineAtOffset(startX, startY);
            contentStream.showText(line.toString());
            contentStream.endText();

            contentStream.close();

            inputStream.close();
        }

        response.setContentType("application/pdf");
        document.save(response.getOutputStream());
        document.close();
    }
}
