# LinkUp Backend

Spring Boot 3 backend converted to an Instagram-like social network model.

## Package Change Plan

- `auth`, `security`: keep JWT authentication. Object-level privacy is enforced in post/story/feed services.
- `user`, `profile`: add `privateAccount`; profile responses include post count, followers count, and following count.
- `connection` -> `follow`: replace mutual connection requests with one-way follows. Public accounts auto-accept follows; private accounts create pending follow requests.
- `post`: support carousel posts, tagged users, hashtags, private visibility, home feed, and explore feed.
- `media`: media rows can belong to a post or story; Cloudinary upload uses quality/format optimization and thumbnail generation.
- `story`: new module for 24-hour image/video stories with scheduled expiration.
- `chat`: keep WebSocket/STOMP and add disappearing messages plus direct sharing of posts/stories.
- `resume`: removed from code and migration drops the `resumes` table.
- `notification`: kept for realtime/user notifications.

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway database migrations
- WebSocket + STOMP
- Maven
- Validation
- Springdoc OpenAPI
- Cloudinary

## Main Features

- Register, login, and current authenticated user lookup
- Public/private account profiles
- One-way follow model with approval for private accounts
- Instagram-style carousel posts
- User tagging in post media
- Hashtag extraction and persistence
- Home feed from followed users
- Explore feed from visible/trending posts
- Stories with 24-hour expiration via `@Scheduled`
- Comments and likes
- Cloudinary image upload with optimized transformations and thumbnails
- Direct messages, disappearing messages, shared post/story messages
- WebSocket realtime chat and notifications

## Setup

```bash
./mvnw clean install
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

## Environment Variables

- `DATABASE_URL`: JDBC PostgreSQL URL, for example `jdbc:postgresql://localhost:5432/linkup`
- `DATABASE_USERNAME`: PostgreSQL username
- `DATABASE_PASSWORD`: PostgreSQL password
- `JWT_SECRET`: at least 32 characters
- `JWT_ISSUER`: default `LinkUp`
- `JWT_AUDIENCE`: default `LinkUpClient`
- `JWT_ACCESS_TOKEN_MINUTES`: default `60`
- `JWT_REFRESH_TOKEN_DAYS`: default `7`
- `CORS_ALLOWED_ORIGINS`: comma-separated frontend origins
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `PORT`: default `8080`
- `JPA_DDL_AUTO`: default `validate`
- `FLYWAY_ENABLED`: default `true`
- `FLYWAY_BASELINE_ON_MIGRATE`: default `true`
- `FLYWAY_BASELINE_VERSION`: default `1`
- `DB_PREPARE_THRESHOLD`: default `0`; keep this at `0` for Supabase pooler/PgBouncer to avoid duplicate prepared statement errors.

## Database Migration

SQL migration file:

- `src/main/resources/db/migration/V1__initial_instagram_schema.sql`
- `src/main/resources/db/migration/V2__instagram_social_model.sql`

Flyway is enabled by default. Hibernate `ddl-auto` defaults to `validate`, so schema changes should be added as SQL migrations instead of relying on Hibernate auto-update.

For an existing database that already has tables but no `flyway_schema_history`, keep `FLYWAY_BASELINE_ON_MIGRATE=true`. For a brand-new database, Flyway will run `V1` then `V2`.

When running against Supabase pooler/PgBouncer, do not set `JPA_DDL_AUTO=update`. Use `JPA_DDL_AUTO=validate` and keep `DB_PREPARE_THRESHOLD=0`.

Important schema changes:

- `connections` becomes `follows`
- `users.private_account` added
- `posts.image_url` and `posts.video_url` replaced by rows in `media`
- `hashtags`, `post_hashtags`, `post_tags`, and `stories` added
- `messages` supports disappearing messages and shared post/story references
- `resumes` dropped

## Expected Tables

- `users`
- `profiles`
- `posts`
- `media`
- `post_tags`
- `hashtags`
- `post_hashtags`
- `stories`
- `comments`
- `likes`
- `follows`
- `conversations`
- `conversation_members`
- `messages`
- `notifications`
- `refresh_tokens`

## Main APIs

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/users/me`
- `GET /api/users/{id}`
- `GET /api/users/search?keyword=...`
- `GET /api/profiles/me`
- `PUT /api/profiles/me`
- `POST /api/follows/{targetUserId}`
- `POST /api/follows/{followerId}/approve`
- `POST /api/follows/{followerId}/decline`
- `DELETE /api/follows/{targetUserId}`
- `GET /api/users/{userId}/followers`
- `GET /api/users/{userId}/following`
- `GET /api/follows/requests`
- `GET /api/users/{userId}/follow-status`
- `POST /api/posts`
- `GET /api/posts/feed`
- `GET /api/posts/explore`
- `GET /api/posts/{id}`
- `PUT /api/posts/{id}`
- `DELETE /api/posts/{id}`
- `GET /api/users/{userId}/posts`
- `GET /api/posts/{postId}/comments`
- `POST /api/posts/{postId}/comments`
- `PUT /api/comments/{commentId}`
- `DELETE /api/comments/{commentId}`
- `POST /api/posts/{postId}/likes`
- `DELETE /api/posts/{postId}/likes`
- `POST /api/stories`
- `GET /api/stories`
- `POST /api/media/images`
- `POST /api/media/avatar`
- `POST /api/conversations`
- `GET /api/conversations`
- `GET /api/conversations/{conversationId}/messages`
- `POST /api/conversations/{conversationId}/messages`
- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/{id}/read`

## WebSocket

- STOMP endpoint: `/ws`
- Send message destination: `/app/chat.send`
- Subscribe to conversation messages: `/topic/conversations/{conversationId}`
- Subscribe to notifications: `/topic/notifications/{userId}`

`/app/chat.send` accepts:

```json
{
  "conversationId": 1,
  "senderId": 1,
  "content": "Hello",
  "attachmentUrl": null,
  "sharedPostId": null,
  "sharedStoryId": null,
  "disappearAfterSeconds": null
}
```

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Render Deployment Notes

- Use Java 21.
- Build command: `./mvnw clean package -DskipTests`
- Start command: `java -jar target/linkup-backend-0.0.1-SNAPSHOT.jar`
- Set all environment variables in Render.
- Use a managed PostgreSQL instance or Supabase PostgreSQL.
- Set `CORS_ALLOWED_ORIGINS` to the deployed frontend domain.
