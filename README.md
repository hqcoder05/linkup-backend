# LinkUp Backend

Spring Boot 3 backend migrated from the Okem Social ASP.NET Core MVC/API project.

## Migration Plan

- Keep backend behavior only: REST APIs, persistence, authentication, file upload metadata, notifications, and realtime chat.
- Drop Razor Views, `wwwroot`, MVC controllers, cookies, SignalR hubs, SQL Server, and ASP.NET/EF Core code.
- Convert EF Core models to JPA entities and expose DTO-based REST APIs using Controller -> Service -> Repository.
- Replace SignalR `/hubs/chat` and notification groups with Spring WebSocket + STOMP at `/ws`.
- Store file URLs and metadata in PostgreSQL. Images upload to Cloudinary. Resume files are represented by an external-storage URL so Supabase Storage or another provider can be plugged in without changing API contracts.

## Source Project Analysis

The ASP.NET source included these backend pieces:

- Entities: `User`, `Post`, `Comment`, `Like`, `FriendRequest`, `Media`, `Conversation`, `ConversationMember`, `Message`, `Notification`, and `RefreshToken`.
- Controllers: auth, users/profile, posts, comments, likes, media, friends/connections, conversations, messages, notifications, plus MVC-only Razor controllers.
- DTOs: register/login/auth response, user/profile, post, comment, conversation, message, and notification DTOs.
- Services/repositories: auth/JWT, user, media, notification, and repositories for users/posts/comments/likes/conversations/messages/notifications.
- Realtime: SignalR `ChatHub`, plus like/comment/notification/call hubs. The core migrated realtime path is chat and notifications via STOMP.
- Auth flow: BCrypt password hashes, JWT access tokens, refresh tokens persisted in DB, and cookie auth for MVC. LinkUp keeps JWT-only API auth.
- Upload logic: local image/video upload under `wwwroot/uploads`. LinkUp replaces this with Cloudinary image upload and external resume file storage metadata.

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- JWT with JJWT
- Spring Data JPA / Hibernate
- PostgreSQL
- WebSocket + STOMP
- Maven
- Lombok dependency available
- Jakarta Validation
- Springdoc OpenAPI / Swagger UI
- Cloudinary image upload

## Features

- Register, login, and current authenticated user lookup
- User profile lookup, search, and profile update
- Post feed, create, update, delete, and user posts
- Comments and likes
- LinkedIn-style connections: request, accept, decline, remove, status, incoming, outgoing, and connection lists
- Cloudinary image upload and avatar update
- Resume metadata upload for PDF/DOC/DOCX external storage
- Conversations and messages
- Realtime message and notification topics
- Global exception handling and common API response envelope

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
- `RESUME_STORAGE_BASE_URL`: base public URL from Supabase Storage or another file provider
- `PORT`: default `8080`
- `JPA_DDL_AUTO`: default `update`

## Database

Create a PostgreSQL database named `linkup`, set the connection variables, then run the app. Hibernate can create/update the schema for development with `JPA_DDL_AUTO=update`.

Expected tables:

- `users`
- `profiles`
- `posts`
- `comments`
- `likes`
- `connections`
- `media`
- `resumes`
- `conversations`
- `conversation_members`
- `messages`
- `notifications`
- `refresh_tokens`

## API Documentation

After startup, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Main APIs

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/users/me`
- `GET /api/users/{id}`
- `GET /api/users/search?keyword=...`
- `GET /api/profiles/me`
- `PUT /api/profiles/me`
- `POST /api/posts`
- `GET /api/posts/feed`
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
- `POST /api/connections/{targetUserId}`
- `POST /api/connections/{requesterId}/accept`
- `POST /api/connections/{requesterId}/decline`
- `DELETE /api/connections/{targetUserId}`
- `GET /api/users/{userId}/connections`
- `GET /api/connections/incoming`
- `GET /api/connections/outgoing`
- `GET /api/users/{userId}/connection-status`
- `POST /api/media/images`
- `POST /api/media/avatar`
- `POST /api/resumes/upload`
- `GET /api/resumes/me`
- `DELETE /api/resumes/{id}`
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

`/app/chat.send` currently accepts:

```json
{
  "conversationId": 1,
  "senderId": 1,
  "content": "Hello",
  "attachmentUrl": null
}
```

## Render Deployment Notes

- Use Java 21.
- Build command: `./mvnw clean package -DskipTests`
- Start command: `java -jar target/linkup-backend-0.0.1-SNAPSHOT.jar`
- Set all environment variables in Render.
- Use a managed PostgreSQL instance or Supabase PostgreSQL.
- Set `CORS_ALLOWED_ORIGINS` to the deployed frontend domain when the frontend is ready.

## Not Fully Migrated Yet

- Refresh-token rotation/logout endpoints are modeled in the database but not exposed yet.
- Original friend-request behavior is now modeled as LinkedIn-style two-way connections.
- SignalR typing/seen/call/like/comment hub events were reduced to core STOMP chat and notification flows.
- Resume files are validated and metadata is saved, but actual Supabase Storage upload should be implemented with a provider SDK or signed upload flow.
- Video upload from the old media API is not included because the requested target only required Cloudinary image upload and resume files.
