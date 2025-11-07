package com.rms.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * POJO đại diện cho một bước trong Flow Builder (UC-DEV-01)
 * [SỬA LỖI NGÀY 29] Triển khai (implements) Serializable
 * để đảm bảo toàn bộ đối tượng Artifact có thể được truyền (pass) vào Dragboard.
 */
public class FlowStep implements Serializable {

    /**
     * ID phiên bản để đảm bảo tính tương thích (compatibility) khi Serializable.
     */
    private static final long serialVersionUID = 1L;

    @JsonProperty("actor")
    private String actor;

    @JsonProperty("action")
    private String action;

    @JsonProperty("logicType")
    private String logicType; // (Ví dụ: "IF", "ELSE", "LOOP")

    @JsonProperty("nestedSteps")
    private List<FlowStep> nestedSteps; // (Cho logic lồng nhau - Ngày 15)

    // --- Getters and Setters ---
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getLogicType() { return logicType; }
    public void setLogicType(String logicType) { this.logicType = logicType; }
    public List<FlowStep> getNestedSteps() { return nestedSteps; }
    public void setNestedSteps(List<FlowStep> nestedSteps) { this.nestedSteps = nestedSteps; }
}