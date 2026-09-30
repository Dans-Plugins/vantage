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
**Authentication:** Required. Pass the JWT as the `token` query parameter when connecting (for
example `ws://localhost:9000/ws/log?token=<jwt>`). The connection is closed with a policy-violation
status if the parameter is missing, the token is invalid or expired, or its user no longer exists.

Messages are JSON objects identified by a `type` field.

Client to server:

| Message | Effect |
|---------|--------|
| `{"type":"command","command":"<command>"}` | Runs the command on the server console if the server is running |
| `{"type":"start"}` | Starts the server if it is not running |
| `{"type":"ping"}` | Answered with a `pong` message |

Server to client:

| Message | Meaning |
|---------|---------|
| `{"type":"log","text":"<line>"}` | A line of server console output |
| `{"type":"status"}` | The server has started or stopped; fetch `GET /api/v2/server` for the new state |
| `{"type":"pong"}` | Reply to a `ping` |
