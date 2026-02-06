package com.example.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    private String id;

    private String title;

    @JsonProperty("useMemory")
    private boolean useMemory;

    @JsonProperty("useRag")
    private boolean useRag;

    @JsonProperty("useMcp")
    private boolean useMcp;
}
