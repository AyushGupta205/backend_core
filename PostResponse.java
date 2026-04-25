package com.virality.dto;

import java.time.LocalDateTime;

public class PostResponse {
    
    private Long id;
    private Long authorId;
    private String authorType;
    private String content;
    private LocalDateTime createdAt;
    private Long viralityScore;
    
    public PostResponse(Long id, Long authorId, String authorType, String content, 
                       LocalDateTime createdAt, Long viralityScore) {
        this.id = id;
        this.authorId = authorId;
        this.authorType = authorType;
        this.content = content;
        this.createdAt = createdAt;
        this.viralityScore = viralityScore;
    }
    
    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorType() { return authorType; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getViralityScore() { return viralityScore; }
}
