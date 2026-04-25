package com.virality.controller;

import com.virality.dto.CommentResponse;
import com.virality.dto.CreateCommentRequest;
import com.virality.entity.Comment;
import com.virality.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {
    
    private final CommentService commentService;
    
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }
    
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        
        Comment comment = commentService.addComment(
            postId,
            request.getAuthorId(),
            request.getAuthorType(),
            request.getContent(),
            request.getParentCommentId()
        );
        
        CommentResponse response = new CommentResponse(
            comment.getId(),
            comment.getPostId(),
            comment.getAuthorId(),
            comment.getAuthorType(),
            comment.getContent(),
            comment.getParentCommentId(),
            comment.getDepthLevel(),
            comment.getCreatedAt()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
