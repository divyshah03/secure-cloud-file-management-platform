# Architecture Overview

## System Architecture

This is a full-stack file management system with the following architecture:

### Backend Architecture (Spring Boot)

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│  (REST Controllers, DTOs, Request/Response)         │
├─────────────────────────────────────────────────────┤
│                 Application Layer                    │
│  (Business Logic, Services, Use Cases)              │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                       │
│  (Entities, Domain Models, Business Rules)          │
├─────────────────────────────────────────────────────┤
│              Infrastructure Layer                    │
│  (Persistence, Storage, Security, Config)           │
└─────────────────────────────────────────────────────┘
```

### Frontend Architecture (React)

```
┌─────────────────────────────────────────────────────┐
│                    Pages Layer                       │
│  (Route Components, Page Components)                │
├─────────────────────────────────────────────────────┤
│                 Components Layer                     │
│  (Reusable UI Components, Layout Components)        │
├─────────────────────────────────────────────────────┤
│                    Hooks Layer                       │
│  (Custom Hooks, Context Providers)                  │
├─────────────────────────────────────────────────────┤
│                    API Layer                         │
│  (HTTP Client, API Services)                        │
├─────────────────────────────────────────────────────┤
│                   Utils Layer                        │
│  (Utility Functions, Helpers)                       │
└─────────────────────────────────────────────────────┘
```

## Package Structure

### Backend Packages

All backend code lives under `com.cloudfilemanager`, organized by feature
rather than by technical layer:

- **auth/**: `AuthController`, `AuthenticationService`, and the auth
  request/response DTOs (`auth/dto/`)
- **user/**: `User` entity, `Role`, `UserRepository`, `UserService`,
  `UserDtoMapper`, `EmailVerificationService`, and user DTOs (`user/dto/`)
- **file/**: `File` entity, `FileController`, `FileService`,
  `FileRepository`, `FileDtoMapper`, and file DTOs (`file/dto/`)
- **storage/**: `S3Service`, `S3Buckets`, `S3Config`, `FakeS3` (local
  filesystem-backed storage for development)
- **email/**: `EmailService` interface, `SmtpEmailService` (JavaMail-backed
  implementation), `NoOpEmailService` (logs instead of sending)
- **security/**: `JwtUtil`, `JwtAuthenticationFilter`, `SecurityConfig`,
  `SecurityFilterChainConfig`, `CorsConfig`, `UserDetailsServiceImpl`,
  `DelegatedAuthEntryPoint`
- **common/**: `PingController` and shared exception types
  (`common/exception/`), including `GlobalExceptionHandler`

### Frontend Structure

- **api/**: API client (`client.js`)
- **components/**: Reusable UI components
  - `file/`: File-related components (`FileCard`, `FileUpload`)
  - `layout/`: Layout components (`Sidebar`, `ProtectedRoute`)
- **context/**: `AuthProvider` — auth context provider and the `useAuth` hook
- **pages/**: Route page components, one file per route (`Login`, `Signup`,
  `EmailVerification`, `Dashboard`, `Files`)
- **utils/**: Utility functions (notifications)

## Data Flow

### File Upload Flow
1. User selects file in React UI
2. File sent to `/api/v1/files` (multipart/form-data)
3. Backend validates file (size, type)
4. File stored in S3 (or FakeS3)
5. Metadata saved to PostgreSQL
6. FileDto returned to frontend

### Authentication Flow
1. User submits credentials
2. Spring Security validates credentials
3. JWT token generated
4. Token returned in response header
5. Frontend stores token in localStorage
6. Token included in subsequent requests
7. JWT filter validates token on each request

## Security Architecture

- **JWT Authentication**: Stateless token-based authentication
- **Password Encryption**: BCrypt password hashing
- **Role-Based Access Control**: USER and ADMIN roles
- **CORS Configuration**: Configurable CORS for frontend
- **Input Validation**: Bean validation on all endpoints
- **Error Handling**: Global exception handler

## Storage Architecture

- **Database**: PostgreSQL for metadata and user data
- **File Storage**: AWS S3 (or FakeS3 for development)
- **File Organization**: `files/{userId}/{uuid}.ext`
- **Metadata**: Stored in `files` table with foreign key to `users`
