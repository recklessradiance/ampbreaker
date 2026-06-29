package com.rockstarpainkiller.ampbreaker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockstarpainkiller.ampbreaker.model.SurvivalGuide;
import com.rockstarpainkiller.ampbreaker.model.SurvivalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdhrustadevathaService {

    private static final Logger log = LoggerFactory.getLogger(AdhrustadevathaService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdhrustadevathaService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory cloudFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        cloudFactory.setConnectTimeout(5000);
        cloudFactory.setReadTimeout(50000);
        this.restTemplate = new RestTemplate(cloudFactory);
    }

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${xai.api.key:}")
    private String xaiApiKey;

    public SurvivalGuide generateSurvivalGuide(SurvivalRequest request, String provider, String customApiKey, String customModel, String language) {
        String providerName = (provider != null) ? provider.toLowerCase() : "cloud";
        String langMode = (language != null) ? language : "English";



        // Cloud provider cascade pipeline
        // 1. Try Gemini
        try {
            log.info("Attending satellite uplink to Gemini API...");
            String keyToUse = (customApiKey != null && !customApiKey.isEmpty()) ? customApiKey : geminiApiKey;
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + keyToUse;
            String prompt = buildPrompt(request, langMode);

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> contentMap = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> partMap = new HashMap<>();
            partMap.put("text", prompt);
            parts.add(partMap);
            contentMap.put("parts", parts);
            contents.add(contentMap);
            requestBody.put("contents", contents);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            
            Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> responseParts = (List<Map<String, Object>>) content.get("parts");
            String generatedJson = (String) responseParts.get(0).get("text");
            return objectMapper.readValue(generatedJson, SurvivalGuide.class);
        } catch (Exception geminiErr) {
            log.warn("Gemini connection throttled/failed. Redirecting to OpenAI (gpt-4o-mini)... error: {}", geminiErr.getMessage());
            
            // 2. Try OpenAI
            try {
                String keyToUse = (customApiKey != null && !customApiKey.isEmpty() && !customApiKey.equals(geminiApiKey)) 
                    ? customApiKey 
                    : openaiApiKey;
                String url = "https://api.openai.com/v1/chat/completions";
                String prompt = buildPrompt(request, langMode);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "gpt-4o-mini");
                
                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> messageMap = new HashMap<>();
                messageMap.put("role", "user");
                messageMap.put("content", prompt);
                messages.add(messageMap);
                requestBody.put("messages", messages);

                Map<String, String> responseFormat = new HashMap<>();
                responseFormat.put("type", "json_object");
                requestBody.put("response_format", responseFormat);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + keyToUse);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
                
                Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String generatedJson = (String) message.get("content");
                return objectMapper.readValue(generatedJson, SurvivalGuide.class);
            } catch (Exception openaiErr) {
                log.warn("OpenAI connection throttled/failed. Redirecting to xAI Grok (grok-beta)... error: {}", openaiErr.getMessage());
                
                // 3. Try Grok
                try {
                    String keyToUse = (customApiKey != null && !customApiKey.isEmpty() && !customApiKey.equals(geminiApiKey)) 
                        ? customApiKey 
                        : xaiApiKey;
                    String url = "https://api.x.ai/v1/chat/completions";
                    String prompt = buildPrompt(request, langMode);

                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("model", "grok-beta");
                    
                    List<Map<String, String>> messages = new ArrayList<>();
                    Map<String, String> messageMap = new HashMap<>();
                    messageMap.put("role", "user");
                    messageMap.put("content", prompt);
                    messages.add(messageMap);
                    requestBody.put("messages", messages);

                    Map<String, String> responseFormat = new HashMap<>();
                    responseFormat.put("type", "json_object");
                    requestBody.put("response_format", responseFormat);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Authorization", "Bearer " + keyToUse);

                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                    ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
                    
                    Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String generatedJson = (String) message.get("content");
                    return objectMapper.readValue(generatedJson, SurvivalGuide.class);
                } catch (Exception grokErr) {
                    log.error("All satellite uplinks failed. Engaging offline core simulation.");
                    return buildMockGuide(request, "Failed all models", langMode);
                }
            }
        }
    }

    private String buildPrompt(SurvivalRequest request, String language) {
        String langInstruction = "";
        if ("Tenglish".equalsIgnoreCase(language)) {
            langInstruction = "\nCRITICAL: You must write the entire output (scenarioName, threatLevel, step titles and descriptions, recommendedGear, and humorousQuote) in Tenglish (a casual, sarcastic mix of Telugu and English, e.g., 'Kangaaru padaku', 'Sachipo').";
        } else if ("Hinglish".equalsIgnoreCase(language)) {
            langInstruction = "\nCRITICAL: You must write the entire output (scenarioName, threatLevel, step titles and descriptions, recommendedGear, and humorousQuote) in Hinglish (a casual, sarcastic mix of Hindi and English, e.g., 'Ghabrao mat', 'Mar jao').";
        }

        return "You are an expert, highly sarcastic, and humorous survival guide generator.\n" +
                "Generate a structured, humorous survival guide with exactly 5 steps for this disaster scenario:\n" +
                "Scenario: " + request.getScenario() + "\n" +
                "Current Location: " + request.getLocation() + "\n" +
                "Inventory (Items the user has): " + String.join(", ", request.getInventory()) + "\n" +
                langInstruction + "\n\n" +
                "The output must be a valid JSON object matching the following structure:\n" +
                "{\n" +
                "  \"scenarioName\": \"Sarcastic name of the scenario\",\n" +
                "  \"threatLevel\": \"A funny rating of the threat level (e.g. Critical, Moderate, Mildly inconvenient)\",\n" +
                "  \"steps\": [\n" +
                "    {\n" +
                "      \"title\": \"Short title of step\",\n" +
                "      \"description\": \"Humorous and highly sarcastic step explanation\",\n" +
                "      \"survivalRateMultiplier\": 1.25\n" +
                "    }\n" +
                "  ],\n" +
                "  \"recommendedGear\": \"A funny description of what they should have brought instead\",\n" +
                "  \"humorousQuote\": \"A witty, sarcastic quote about surviving or failing in this scenario\"\n" +
                "}";
    }

    private SurvivalGuide buildMockGuide(SurvivalRequest request, String errorMessage, String langMode) {
        SurvivalGuide guide = new SurvivalGuide();
        String gearList = String.join(" and ", request.getInventory());
        if (gearList.isEmpty()) {
            gearList = "your bare hands";
        }
        
        List<com.rockstarpainkiller.ampbreaker.model.SurvivalStep> steps = new ArrayList<>();
        
        if ("Tenglish".equalsIgnoreCase(langMode)) {
            guide.setScenarioName("[OFFLINE] INKA EVADU NINNU KAPADALEDU.");
            guide.setThreatLevel("ASALU KANAPADATLEDU");
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step1 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step1.setTitle("Kangaaru Padi Sachipo");
            step1.setDescription("Nee badhalu chusi navvukodaaniki AI devullu kuda offline poyaru. Satellites anni fasak. Inka evadu ninnu kapadaledu.");
            step1.setSurvivalRateMultiplier(0.00);
            steps.add(step1);
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step2 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step2.setTitle("Cheddi Chetta Vethuko");
            step2.setDescription("Nuvvu \"" + request.getLocation() + "\" daggara stuck ayyav, adi kuda nee sanchi lo unna [" + gearList + "] tho. Ee chetta tho em chesthav ra nanna? Emi cheyalev.");
            step2.setSurvivalRateMultiplier(0.05);
            steps.add(step2);
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step3 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step3.setTitle("Deeni Valla Chala Nastam");
            step3.setDescription("Prathi chinna vishayaniki stress ayyi em labham ledhu. Gunde aagipoye risk undhi.");
            step3.setSurvivalRateMultiplier(0.10);
            steps.add(step3);

            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step4 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step4.setTitle("Devudni Thaluchuko");
            step4.setDescription("Inka ninnu aa paina unna devude kapadaali. Gudi metlu ekki dhandam pettuko.");
            step4.setSurvivalRateMultiplier(0.25);
            steps.add(step4);

            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step5 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step5.setTitle("Pranathaalu Vadhilesi Paduko");
            step5.setDescription("Prasanthamga kurchuni goyya thavvuko. Poye kaalam vacchindi.");
            step5.setSurvivalRateMultiplier(1.00);
            steps.add(step5);
            
            guide.setRecommendedGear("Pani chese wifi signal ledha dabbulunna developer account");
            guide.setHumorousQuote("Network ledhu. Key ledhu. Inka chusi chavu!");
        } else if ("Hinglish".equalsIgnoreCase(langMode)) {
            guide.setScenarioName("[OFFLINE] AB KOI TUMHARI MADAD NAHI KAR SAKTA.");
            guide.setThreatLevel("BILKUL ZERO");
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step1 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step1.setTitle("Ghabrao aur Haar Mano");
            step1.setDescription("Sare AI devta offline chale gaye hain. Satellites ka dabba gul ho gaya hai. Ab koi tumhari madad nahi kar sakta.");
            step1.setSurvivalRateMultiplier(0.00);
            steps.add(step1);
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step2 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step2.setTitle("Apna Kachra Dekho");
            step2.setDescription("Tum \"" + request.getLocation() + "\" par stuck ho aur tumhare jhole mein [" + gearList + "] hai. Is kachre se kya hi hoga? Kuch nahi.");
            step2.setSurvivalRateMultiplier(0.05);
            steps.add(step2);
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step3 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step3.setTitle("Rona Shuru Karo");
            step3.setDescription("Aise situation mein rona hi ek aakhri option bachta hai. Aansu bahaao.");
            step3.setSurvivalRateMultiplier(0.10);
            steps.add(step3);

            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step4 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step4.setTitle("Bhagwan ko Yaad Karo");
            step4.setDescription("Bhagwan ke darbaar mein arzi lagao aur shanti se baithe raho.");
            step4.setSurvivalRateMultiplier(0.25);
            steps.add(step4);

            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step5 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step5.setTitle("Aakhri Dua Mango");
            step5.setDescription("Shanti se baitho aur aaram se apna khel khatam hone ka wait karo.");
            step5.setSurvivalRateMultiplier(1.00);
            steps.add(step5);
            
            guide.setRecommendedGear("Chalta hua internet connection ya paid AI account");
            guide.setHumorousQuote("Uplink gayab. Gyaan khatam. Ab dekho aur maro!");
        } else {
            guide.setScenarioName("[OFFLINE] NO ONE CAN HELP YOU.");
            guide.setThreatLevel("ABSOLUTE ZERO");
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step1 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step1.setTitle("Panic and Accept Fate");
            step1.setDescription("Your current disaster \"" + request.getScenario() + "\" is absolute. The network links are dead, the satellites are fried, and frankly, no one can help you.");
            step1.setSurvivalRateMultiplier(0.00);
            steps.add(step1);
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step2 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step2.setTitle("Assess Your Trash");
            step2.setDescription("You are stuck at \"" + request.getLocation() + "\" with: [" + gearList + "]. Think about how this junk is going to save you. Hint: it won't.");
            step2.setSurvivalRateMultiplier(0.05);
            steps.add(step2);
            
            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step3 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step3.setTitle("Shed Some Tears");
            step3.setDescription("When all else fails, a good crying session can temporarily relieve stress. Do it now.");
            step3.setSurvivalRateMultiplier(0.10);
            steps.add(step3);

            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step4 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step4.setTitle("Make Peace With the Universe");
            step4.setDescription("Sit back and think of all your life regrets. You'll have plenty of time for that.");
            step4.setSurvivalRateMultiplier(0.25);
            steps.add(step4);

            com.rockstarpainkiller.ampbreaker.model.SurvivalStep step5 = new com.rockstarpainkiller.ampbreaker.model.SurvivalStep();
            step5.setTitle("Prepare for Impact");
            step5.setDescription("Find a comfortable spot, sit down, and wait for the inevitable doom.");
            step5.setSurvivalRateMultiplier(1.00);
            steps.add(step5);
            
            guide.setRecommendedGear("A working internet connection or an active paid AI account");
            guide.setHumorousQuote("Uplinks offline. Telemetry lost. No one can help you.");
        }
        
        guide.setSteps(steps);
        return guide;
    }
}
