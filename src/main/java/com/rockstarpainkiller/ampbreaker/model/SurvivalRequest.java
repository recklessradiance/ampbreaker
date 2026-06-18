package com.rockstarpainkiller.ampbreaker.model;

import lombok.Data;
import java.util.List;

@Data
public class SurvivalRequest {
    private String scenario;
    private String location;
    private List<String> inventory;
}
