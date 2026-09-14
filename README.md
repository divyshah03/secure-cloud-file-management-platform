# Cloud File Manager

A secure, scalable file management web application built with Spring Boot and React.js, enabling users to upload, view, and delete files with robust authentication and cloud storage integration.

## 🚀 Features

- **Secure File Management**: Upload, view, and delete files with user-specific access control
- **JWT Authentication**: Secure authentication using JSON Web Tokens and Spring Security
- **Role-Based Access Control**: Data isolation ensuring users can only access their own files
- **Cloud Storage**: AWS S3 integration for scalable file storage
- **PostgreSQL Database**: Persistent metadata storage for files and user information
- **Email Verification**: User signup with email verification flow
- **File Size Support**: Handle files up to 50 MB

## 🛠️ Tech Stack

### Backend
- **Spring Boot 3** (Java 17)
- **Spring Security** with JWT
- **PostgreSQL** - Database
- **AWS S3** - Cloud storage
- **Flyway** - Database migrations
- **Maven** - Build tool

### Frontend
- **React.js** - Frontend framework
- **Axios** - HTTP client
- **Chakra UI** - UI components
- **Vite** - Build tool

## 📋 Prerequisites

- Java 17 or higher
- Node.js 16+ and npm
- PostgreSQL 14+
- Maven 3.6+
- AWS Account (for S3) or use local mock storage

## 🔧 Setup Instructions

### Quick start with Docker

```bash
docker compose up --build
```

Frontend: `http://localhost:3000` · Backend: `http://localhost:8080`

### Backend Setup

1. **Configure Database**
   ```bash
   # Update database credentials in backend/src/main/resources/application.yml
   ```

2. **Configure AWS S3** (Optional - can use mock storage)
   ```bash
   # Set environment variables:
   export AWS_S3_MOCK=true  # Use local mock storage for development
   export AWS_REGION=us-east-1
   export AWS_S3_BUCKET_FILES=your-bucket-name
   ```

3. **Build and Run**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```
   Backend will run on `http://localhost:8080`

### Frontend Setup

1. **Install Dependencies**
   ```bash
   cd frontend
   npm install
   ```

2. **Configure API URL**
   ```bash
   cp .env.example .env
   # Edit VITE_API_BASE_URL if your backend is not on http://localhost:8080
   ```

3. **Run Development Server**
   ```bash
   npm run dev
   ```
   Frontend will run on `http://localhost:5173`

## 🔐 Authentication

- User registration with email verification
- JWT-based authentication
- Role-based access control (USER, ADMIN)
- Secure password storage with BCrypt

## 📁 Project Structure

```
backend/
├── src/main/java/com/cloudfilemanager/
│   ├── auth/            # Login, AuthenticationService, auth request/response DTOs
│   ├── user/            # User entity, repository, service, email verification, DTOs
│   ├── file/            # File entity, repository, service, controller, DTOs
│   ├── storage/         # S3Service, S3Buckets, FakeS3 (local dev storage)
│   ├── email/           # EmailService interface, SmtpEmailService, NoOpEmailService
│   ├── security/        # JWT filter/util, Spring Security config, CORS
│   └── common/          # Shared exceptions, global exception handler, ping controller

frontend/
├── src/
│   ├── api/            # API client
│   ├── components/     # Reusable UI components (file/, layout/, forms/)
│   ├── pages/          # Route page components
│   └── utils/          # Utility functions
```

## 🧪 Testing

- Backend unit tests with JUnit and Testcontainers
- File uploads up to 50 MB
- Role-based access control validation

## 📝 API Endpoints

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User authentication
- `POST /api/v1/auth/verify-email` - Email verification
- `GET /api/v1/files` - List user files
- `POST /api/v1/files` - Upload file
- `GET /api/v1/files/{id}/download` - Download file
- `DELETE /api/v1/files/{id}` - Delete file

## 🌐 Deployment

- Backend deployed on cloud platform (Spring Boot application)
- Frontend deployed as static site
- PostgreSQL database instance
- AWS S3 bucket for file storage

## 📄 License

This project is part of a portfolio demonstration.
