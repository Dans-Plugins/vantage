# API Reference

Vantage is a web-based management panel and does not use in-game commands. Instead, it exposes a REST API and a WebSocket endpoint. All endpoints (except login) require authentication via a JWT bearer token.

## Authentication

### POST /api/v2/login

**Description:** Authenticate a user and obtain a JWT token.
**Authentication:** None required.
**Request Body:** JSON with `username` and `password` fields.
**Response:** JWT token on success.

## User Endpoints

### GET /api/v2/user/{id}

**Description:** Get details of a specific user.
**Authentication:** Required.

### POST /api/v2/user/

**Description:** Create a new user.
**Authentication:** Required.

### PATCH /api/v2/user/{id}

**Description:** Update an existing user.
**Authentication:** Required.

### GET /api/v2/users

**Description:** List all users.
**Authentication:** Required.

## Server Endpoints

### GET /api/v2/server

**Description:** Get the current server status.
**Authentication:** Required.

### GET /api/v2/server/log

**Description:** Get recent server log entries.
**Authentication:** Required.

## Audit Endpoints

### GET /api/v2/audit/{page}

**Description:** Get a paginated list of audit log entries.
**Authentication:** Required.

## File Endpoints

### GET /api/v2/files/\[{path}\]

**Description:** List files or get file contents at the given path.
**Authentication:** Required.

### PUT /api/v2/files/\[{path}\]

**Description:** Upload or update a file at the given path.
**Authentication:** Required.

### DELETE /api/v2/files/{path}

**Description:** Delete a file at the given path.
**Authentication:** Required.

## Brand Endpoint

### GET /api/v2/brand

**Description:** Get server branding information (server name).
**Authentication:** Required.

## WebSocket

### /ws/log

**Description:** Live server console log stream via WebSocket.
**Authentication:** Required (token sent as a WebSocket message after connection).
