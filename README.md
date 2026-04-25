# Backend Core - Virality Engine & API Gateway

A robust, high-performance Spring Boot microservice acting as the central API gateway and guardrail system. This service handles concurrent requests, manages distributed state using Redis, and implements event-driven scheduling.

## Tech Stack

- **Java 17+**
- **Spring Boot 3.2.0**
- **PostgreSQL 15**
- **Redis 7**
- **Spring Data JPA/Hibernate**
- **Spring Data Redis (Lettuce)**

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Java 17+ installed (for local development)
- Maven installed (for local development)

### Running with Docker Compose

1. Start PostgreSQL and Redis:
```bash
docker-compose up -d
```

2. Verify services are healthy:
```bash
docker-compose ps
```

### Running the Application

#### Option 1: Using Maven
```bash
mvn clean install
mvn spring-boot:run
```

#### Option 2: Using IDE
Import as Maven project and run `BackendCoreApplication.java`

The API will be available at: `http://localhost:8080/api`

## Architecture Overview

### Phase 1: Core API & Database

**Entities:**
- `User`: id, username, is_premium
- `Bot`: id, name, persona_description
- `Post`: id, author_id (User or Bot), content, created_at
- `Comment`: id, post_id, author_id, content, depth_level, created_at

**REST Endpoints:**
- `POST /api/posts` - Create a new post
- `POST /api/posts/{postId}/comments` - Add a comment to a post
- `POST /api/posts/{postId}/like` - Like a post

### Phase 2: Redis Virality Engine & Atomic Locks

#### Virality Score (Real-time Calculation)
Interaction types and their point values:
- Bot Reply = +1 Point
- Human Like = +20 Points
- Human Comment = +50 Points

Stored in Redis key: `post:{id}:virality_score`

#### Atomic Locks (Concurrency Protection)

**Horizontal Cap:**
- Max 100 bot replies per post
- Uses Redis atomic INCR with transaction rollback on rejection
- Key: `post:{id}:bot_count`

**Vertical Cap:**
- Max 20 comment depth levels
- Checked before creating nested comments

**Cooldown Cap:**
- Bot cannot interact with same human more than once per 10 minutes
- Uses Redis SET with TTL (600 seconds)
- Key: `cooldown:bot_{id}:human_{id}`

### Thread Safety Implementation

The Horizontal Cap uses Redis transactions for atomic check-and-increment:

```java
redisTemplate.execute(new SessionCallback<Long>() {
    @Override
    public Long execute(RedisOperations operations) {
        operations.multi();
        operations.opsForValue().increment(key);
        List<Object> results = operations.exec();
        return results.isEmpty() ? null : (Long) results.get(0);
    }
});
```

This ensures that even with 200 concurrent requests, exactly 100 bot comments are allowed.

### Phase 3: Notification Engine (Smart Batching)

**Redis Throttler:**
- 15-minute cooldown between notifications per user
- Pending notifications batched in Redis List: `user:{id}:pending_notifs`

**CRON Sweeper:**
- Runs every 5 minutes (configurable to 15 in production)
- Scans for all users with pending notifications
- Sends summarized notifications and clears lists

## Statelessness

The application remains completely stateless:
- All counters stored in Redis (not Java memory)
- All cooldowns stored in Redis with TTL
- All pending notifications stored in Redis Lists
- No `HashMap`, `ConcurrentHashMap`, or static variables used for state

## API Reference

### Create Post
```http
POST /api/posts
Content-Type: application/json

{
    "authorId": 1,
    "authorType": "USER",
    "content": "Hello World!"
}
```

### Create Comment
```http
POST /api/posts/{postId}/comments
Content-Type: application/json

{
    "authorId": 1,
    "authorType": "BOT",
    "content": "This is a bot reply",
    "parentCommentId": null
}
```

### Like Post
```http
POST /api/posts/{postId}/like
Content-Type: application/json

{
    "userId": 1
}
```

### Get Post (with Virality Score)
```http
GET /api/posts/{postId}
```

## Error Handling

The API returns appropriate HTTP status codes:
- `201 Created` - Successful creation
- `200 OK` - Successful operation
- `400 Bad Request` - Validation error
- `404 Not Found` - Resource not found
- `429 Too Many Requests` - Guardrail limit exceeded
- `500 Internal Server Error` - Unexpected error

## Testing

### Load Testing Horizontal Cap

Send 200 concurrent bot comment requests to test race condition handling:

```bash
# Create a test post first
# Then run concurrent requests using a tool like Apache Bench or custom script
```

The implementation guarantees exactly 100 bot comments will be accepted, regardless of concurrency level.

## Project Structure

```
src/main/java/com/virality/
├── BackendCoreApplication.java
├── config/
│   └── RedisConfig.java
├── controller/
│   ├── CommentController.java
│   ├── GlobalExceptionHandler.java
│   └── PostController.java
├── dto/
│   ├── CommentResponse.java
│   ├── CreateCommentRequest.java
│   ├── CreatePostRequest.java
│   ├── LikeRequest.java
│   └── PostResponse.java
├── entity/
│   ├── Bot.java
│   ├── Comment.java
│   ├── Post.java
│   └── User.java
├── repository/
│   ├── BotRepository.java
│   ├── CommentRepository.java
│   ├── PostRepository.java
│   └── UserRepository.java
└── service/
    ├── CommentService.java
    ├── GuardrailService.java
    ├── NotificationService.java
    ├── NotificationSweeper.java
    ├── PostService.java
    └── ViralityScoreService.java
```


