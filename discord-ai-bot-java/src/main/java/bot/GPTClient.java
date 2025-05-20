package bot.ai;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * GPTClient is responsible for communicating with the OpenAI Chat Completions API.
 * It builds the request payload, sends a POST request, and returns the AI-generated response.
 */
public class GPTClient {
    // The endpoint URL for OpenAI's Chat Completions API (no trailing brace)
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    // OkHttp client used to execute HTTP requests
    private final OkHttpClient http;
    // Gson instance for JSON serialization/deserialization
    private final Gson gson;
    // API key for authenticating with OpenAI
    private final String apiKey;

    /**
     * Constructs a new GPTClient with the provided API key.
     *
     * @param apiKey the OpenAI API key
     */
    public GPTClient(String apiKey) {
        this.http = new OkHttpClient();               // HTTP client initialization
        this.gson = new GsonBuilder().create();       // Gson for handling JSON
        this.apiKey = apiKey;                         // Store the API key for future requests
    }

    /**
     * Sends a chat completion request to OpenAI and returns the generated content.
     *
     * @param messages the list of messages in the conversation (each with "role" and "content")
     * @param model    the model name to use (e.g., "gpt-3.5-turbo")
     * @return the assistant's response content
     * @throws IOException if the HTTP call fails or returns a non-success status
     */
    public String ask(List<Map<String, String>> messages, String model) throws IOException {
        // 1) Build the JSON payload for the request
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);          // Set the model to use (e.g., gpt-3.5-turbo)

        JsonArray arr = new JsonArray();              // Array to hold the message history
        for (Map<String, String> msg : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("role", msg.get("role"));         // e.g., "user", "assistant", or "system"
            obj.addProperty("content", msg.get("content"));   // actual text content
            arr.add(obj);
        }
        payload.add("messages", arr);
        String jsonPayload = payload.toString();      // Convert payload to JSON string

        // 2) Debug: print the endpoint URL and the JSON payload
        System.out.println("🔗 OpenAI URL: " + ENDPOINT);
        System.out.println("📦 Payload: " + jsonPayload);

        // 3) Create the request body with the JSON payload
        RequestBody body = RequestBody.create(
                jsonPayload,
                MediaType.get("application/json; charset=utf-8")
        );

        // 4) Build the HTTP POST request with authorization header
        Request request = new Request.Builder()
                .url(ENDPOINT)                                     // API endpoint
                .addHeader("Authorization", "Bearer " + apiKey)   // Authentication using bearer token
                .addHeader("Content-Type", "application/json")    // Indicate we’re sending JSON
                .post(body)                                       // Use POST method
                .build();

        // 5) Execute the request and capture the response
        try (Response resp = http.newCall(request).execute()) {
            int code = resp.code();  // HTTP status code
            String respBody = resp.body() != null ? resp.body().string() : "";

            // Debug: print the HTTP status code and full response body
            System.out.println("🔄 Response code: " + code);
            System.out.println("📬 Response body: " + respBody);

            // Throw an exception if the request was not successful
            if (!resp.isSuccessful()) {
                throw new IOException("Unexpected response from OpenAI: " + code);
            }

            // 6) Parse the JSON response and extract the assistant's message content
            JsonObject root = gson.fromJson(respBody, JsonObject.class);
            return root
                    .getAsJsonArray("choices")             // Get the "choices" array
                    .get(0).getAsJsonObject()              // Take the first choice
                    .getAsJsonObject("message")            // Access the "message" object
                    .get("content").getAsString()          // Extract the assistant's reply
                    .trim();
        }
    }
}
