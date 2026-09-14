# API Documentation

## Base URL
```
Development: http://localhost:8080
Production: https://your-domain.com
```

## Authentication

All endpoints except `/api/v1/auth/*` require authentication via JWT token in the `Authorization` header:

```
Authorization: Bearer <jwt-token>
```

## Authentication Endpoints

### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
- `201 Created` - User registered, verification email sent
- `409 Conflict` - Email already exists
- `400 Bad Request` - Validation errors

### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
- `200 OK` - Returns JWT token in `Authorization` header
- `401 Unauthorized` - Invalid credentials or email not verified

### Verify Email
```http
GET /api/v1/auth/verify-email?token=<verification-token>
```

Or:

```http
POST /api/v1/auth/verify-email
Content-Type: application/json

{
  "token": "<verification-token>"
}
```

### Resend Verification Email
```http
POST /api/v1/auth/resend-verification
Content-Type: application/json

{
  "email": "john@example.com"
}
```

## File Management Endpoints

### Upload File
```http
POST /api/v1/files
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: <binary-file-data>
```

**Response:**
```json
{
  "fileId": 1,
  "fileName": "uuid-generated-name.pdf",
  "originalFileName": "document.pdf",
  "fileSize": 1024000,
  "contentType": "application/pdf",
  "message": "File uploaded successfully"
}
```

### List Files (Paginated)
```http
GET /api/v1/files?page=0&size=20&sortBy=createdAt&sortDir=DESC
Authorization: Bearer <token>
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "fileName": "uuid-generated-name.pdf",
      "originalFileName": "document.pdf",
      "fileSize": 1024000,
      "contentType": "application/pdf",
      "downloadUrl": "/api/v1/files/1/download",
      "createdAt": "2024-01-10T12:00:00",
      "ownerId": 1
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

### Get File Metadata
```http
GET /api/v1/files/{fileId}
Authorization: Bearer <token>
```

### Download File
```http
GET /api/v1/files/{fileId}/download
Authorization: Bearer <token>
```

**Response:**
- `200 OK` - File binary data
- `404 Not Found` - File not found or access denied

### Delete File
```http
DELETE /api/v1/files/{fileId}
Authorization: Bearer <token>
```

**Response:**
- `200 OK` - File deleted successfully
- `404 Not Found` - File not found or access denied

### Get File Statistics
```http
GET /api/v1/files/stats
Authorization: Bearer <token>
```

**Response:**
```json
{
  "fileCount": 10,
  "totalSize": 52428800,
  "totalSizeMB": "50.00"
}
```

## Error Responses

All errors follow this format:

```json
{
  "path": "/api/v1/files",
  "message": "File size exceeds maximum allowed size of 50 MB",
  "statusCode": 400,
  "timestamp": "2024-01-10T12:00:00"
}
```

### HTTP Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created
- `400 Bad Request` - Validation error or invalid request
- `401 Unauthorized` - Authentication required or invalid
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error
