package com.virality.service;

import com.virality.entity.Bot;
import com.virality.entity.Comment;
import com.virality.entity.Post;
import com.virality.repository.BotRepository;
import com.virality.repository.CommentRepository;
import com.virality.repository.PostRepository;
import com.virality.service.ViralityScoreService.InteractionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final BotRepository botRepository;
    private final ViralityScoreService viralityScoreService;
    private final GuardrailService guardrailService;
    private final NotificationService notificationService;
    
    public CommentService(CommentRepository commentRepository,
                         PostRepository postRepository,
                         BotRepository botRepository,
                         ViralityScoreService viralityScoreService,
                         GuardrailService guardrailService,
                         NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.botRepository = botRepository;
        this.viralityScoreService = viralityScoreService;
        this.guardrailService = guardrailService;
        this.notificationService = notificationService;
    }
    
    @Transactional
    public Comment addComment(Long postId, Long authorId, String authorType, 
                            String content, Long parentCommentId) {
        // Validate post exists
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + postId));
        
        // Calculate depth level
        Integer depthLevel = 0;
        if (parentCommentId != null) {
            Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found: " + parentCommentId));
            depthLevel = parent.getDepthLevel() + 1;
        }
        
        // Check vertical cap (max depth)
        guardrailService.checkVerticalCap(depthLevel);
        
        boolean isBot = "BOT".equals(authorType);
        Long humanTargetId = null;
        String botName = null;
        
        if (isBot) {
            Bot bot = botRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot not found: " + authorId));
            botName = bot.getName();
            
            // Determine human target for cooldown check and notification
            if (parentCommentId != null) {
                Comment parent = commentRepository.findById(parentCommentId).orElse(null);
                if (parent != null && "USER".equals(parent.getAuthorType())) {
                    humanTargetId = parent.getAuthorId();
                }
            }
            
            // If no parent or parent is bot, target the post author
            if (humanTargetId == null) {
                if ("USER".equals(post.getAuthorType())) {
                    humanTargetId = post.getAuthorId();
                }
            }
            
            // Apply guardrails only if there's a human target
            if (humanTargetId != null) {
                // Check horizontal cap first (atomic operation)
                guardrailService.checkHorizontalCap(postId, authorId);
                
                // Check cooldown cap
                guardrailService.checkCooldown(authorId, humanTargetId);
                
                // Handle notification (batching)
                notificationService.handleBotInteraction(humanTargetId, botName);
            } else {
                // Still check horizontal cap even if no human target
                guardrailService.checkHorizontalCap(postId, authorId);
            }
            
            // Update virality score for bot reply
            viralityScoreService.updateViralityScore(postId, InteractionType.BOT_REPLY);
        } else {
            // Human comment - update virality score
            viralityScoreService.updateViralityScore(postId, InteractionType.HUMAN_COMMENT);
        }
        
        Comment comment = new Comment(postId, authorId, authorType, content, parentCommentId, depthLevel);
        return commentRepository.save(comment);
    }
}
