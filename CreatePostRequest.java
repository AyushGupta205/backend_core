package com.virality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePostRequest {
    
    @NotNull
    private Long authorId;
    
    @NotBlank
    private String authorType;
    
    @NotBlank
    private String content;
    
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    
    public String getAuthorType() { return authorType; }
    public void setAuthorType(String authorType) { this.authorType = authorType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
