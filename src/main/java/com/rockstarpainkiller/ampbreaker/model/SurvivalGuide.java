package com.rockstarpainkiller.ampbreaker.model;

import lombok.Data;
import java.util.List;

@Data
public class SurvivalGuide {
    private String scenarioName;
    private String threatLevel;
    private List<SurvivalStep> steps;
    private String recommendedGear;
    private String humorousQuote;
}
