package com.virality.service;

import com.virality.entity.Post;
import com.virality.repository.PostRepository;
import com.virality.service.ViralityScoreService.InteractionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    
    private final PostRepository postRepository;
    private final ViralityScoreService viralityScoreService;
    
    public PostService(PostRepository postRepository, ViralityScoreService viralityScoreService) {
        this.postRepository = postRepository;
        this.viralityScoreService = viralityScoreService;
    }
    
    @Transactional
    public Post createPost(Long authorId, String authorType, String content) {
        Post post = new Post(authorId, authorType, content);
        return postRepository.save(post);
    }
    
    @Transactional
    public void likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        
        // Update virality score for human like
        viralityScoreService.updateViralityScore(postId, InteractionType.HUMAN_LIKE);
    }
    
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
    }
    
    public Long getViralityScore(Long postId) {
        return viralityScoreService.getViralityScore(postId);
    }
}
