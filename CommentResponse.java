package com.virality.dto;

import java.time.LocalDateTime;

public class CommentResponse {
    
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorType;
    private String content;
    private Long parentCommentId;
    private Integer depthLevel;
    private LocalDateTime createdAt;
    
    public CommentResponse(Long id, Long postId, Long authorId, String authorType, 
                          String content, Long parentCommentId, Integer depthLevel, 
                          LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.authorType = authorType;
        this.content = content;
        this.parentCommentId = parentCommentId;
        this.depthLevel = depthLevel;
        this.createdAt = createdAt;
    }
    
    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorType() { return authorType; }
    public String getContent() { return content; }
    public Long getParentCommentId() { return parentCommentId; }
    public Integer getDepthLevel() { return depthLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
