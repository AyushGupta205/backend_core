package com.virality.controller;

import com.virality.dto.CreatePostRequest;
import com.virality.dto.LikeRequest;
import com.virality.dto.PostResponse;
import com.virality.entity.Post;
import com.virality.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {
    
    private final PostService postService;
    
    public PostController(PostService postService) {
        this.postService = postService;
    }
    
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        Post post = postService.createPost(
            request.getAuthorId(), 
            request.getAuthorType(), 
            request.getContent()
        );
        
        PostResponse response = new PostResponse(
            post.getId(),
            post.getAuthorId(),
            post.getAuthorType(),
            post.getContent(),
            post.getCreatedAt(),
            0L
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId, 
                                         @Valid @RequestBody LikeRequest request) {
        postService.likePost(postId, request.getUserId());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        Post post = postService.getPost(postId);
        Long viralityScore = postService.getViralityScore(postId);
        
        PostResponse response = new PostResponse(
            post.getId(),
            post.getAuthorId(),
            post.getAuthorType(),
            post.getContent(),
            post.getCreatedAt(),
            viralityScore
        );
        
        return ResponseEntity.ok(response);
    }
}
