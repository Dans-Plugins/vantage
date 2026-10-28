# API Reference

Vantage is a web-based management panel and does not use in-game commands. Instead, it exposes a REST API and a WebSocket endpoint. All endpoints (except login) require authentication via a JWT bearer token. REST endpoints take it as an `Authorization: Bearer <token>` header (the WebSocket takes it as a query parameter; see below). A REST request with a missing, malformed, invalid, or expired token, or whose user no longer exists or is inactive, receives `401 Unauthorized`.

## Authentication

### POST /api/v2/login

**Description:** Authenticate a user and obtain a JWT token.
**Authentication:** None required.
**Request Body:** JSON with `username` and `password` fields.
**Response:** On success, `200 OK` with JSON fields `token` (the JWT), `userId`, and `username`. The token expires 24 hours after it is issued. Returns `401 Unauthorized` if the username is unknown, the password is wrong, or the user is inactive. A successful login is recorded in the audit log as "Logged in".

## User Endpoints

### GET /api/v2/user/{id}

**Description:** Get details of a specific user.
**Authentication:** Required.
**Response:** `200 OK` with JSON fields `id`, `version`, `username`, and `status` (`ACTIVE` or `INACTIVE`). Password hashes and salts are never included. Returns `404 Not Found` if no user has that id.

### POST /api/v2/user/

**Description:** Create a new user.
**Authentication:** Required.
**Request Body:** JSON with `username` and `password` fields.
**Response:** `201 Created` with a `Location` header of `<server.baseUrl>/api/v2/user/<id>` and the new user in the same shape as `GET /api/v2/user/{id}`. New users are `ACTIVE` at version `1`. The creation is recorded in the audit log as "Created user <username> (<id>)".

### PATCH /api/v2/user/{id}

**Description:** Update an existing user.
**Authentication:** Required.
**Request Body:** JSON with a required `version` (the user's current version) and optional `username`, `password`, and `status` fields; omitted fields are left unchanged.
**Response:** `200 OK` with the updated user, whose `version` has gone up by one. The update is recorded in the audit log as "Updated user <username> (<id>)". Returns `404 Not Found` if no user has that id. A `version` that does not match the stored one changes nothing and currently returns `500 Internal Server Error`, not `409 Conflict`.

### GET /api/v2/users

**Description:** List users, ordered by username.
**Authentication:** Required.
**Response:** `200 OK` with JSON fields `users` (the first 50 users, each in the same shape as `GET /api/v2/user/{id}`) and `pagination.totalItems` (the total number of users). The route takes no page parameter, so users beyond the first 50 cannot be listed.

## Server Endpoints

### GET /api/v2/server

**Description:** Get the current server status.
**Authentication:** Required.
**Response:** `200 OK` with JSON field `isRunning`, `true` while the Minecraft server process is running and `false` otherwise.

### GET /api/v2/server/log

**Description:** Get the server console log.
**Authentication:** Required.
**Response:** `200 OK` with JSON field `log`, a list of every line the Minecraft server process has written to standard output since the backend started, oldest first; standard error is not captured. The list is never trimmed or paged.

## Audit Endpoints

### GET /api/v2/audit/{page}

**Description:** Get a paginated list of audit log entries, newest first.
**Authentication:** Required.
**Response:** `200 OK` with JSON fields `items` and `pagination.totalItems` (the total number of audit log entries). Each page holds up to 50 entries, and `{page}` counts from `0`; a `{page}` that is not a number is treated as `0`. Each item has a `user` (in the same shape as `GET /api/v2/user/{id}`), a `description`, and a `time` as an ISO-8601 string.

## File Endpoints

`{path}` is relative to the configured `minecraft.directory` (see [CONFIG.md](CONFIG.md)) and is a
single path segment: address a nested path by encoding each `/` as `%2F`, for example
`/api/v2/files/plugins%2Fconfig.yml`. File operations are not recorded in the audit log.

The containment check that `GET` and `PUT` apply does not normalize `..`, so a `{path}` of `..`
reaches the parent of `minecraft.directory`. `DELETE` applies no containment check at all.

### GET /api/v2/files/\[{path}\]

**Description:** List files or get file contents at the given path.
**Authentication:** Required.
**Response:** For a directory (or no `{path}`, meaning `minecraft.directory` itself), `200 OK` with
JSON field `files`, a list of entries with `name` and `isDirectory`; directories come first, then
files, each sorted by name. For a file, `200 OK` with the file's raw contents as the body. A path
that does not exist fails with an unhandled server error rather than `404 Not Found`.

### PUT /api/v2/files/\[{path}\]

**Description:** Upload or update a file at the given path.
**Authentication:** Required.
**Request Body:** The raw file contents.
**Response:** `204 No Content` once the body has been written, creating the file or replacing its
contents. Returns `400 Bad Request` if the parent directory does not exist; it is not created.
A `PUT` without a `{path}` targets `minecraft.directory` itself and fails with an unhandled server
error.

### DELETE /api/v2/files/{path}

**Description:** Delete a file at the given path.
**Authentication:** Required.
**Response:** `204 No Content`. A directory is deleted together with everything in it. A path that
does not exist also returns `204 No Content`.

## Brand Endpoint

### GET /api/v2/brand

**Description:** Get server branding information (server name).
**Authentication:** Required.
**Response:** `200 OK` with JSON field `serverName`, the configured `server.name` (see [CONFIG.md](CONFIG.md)).

## WebSocket

### /ws/log

**Description:** Live server console log stream via WebSocket.
**Authentication:** Required. Pass the JWT as the `token` query parameter when connecting (for
example `ws://localhost:9000/ws/log?token=<jwt>`). The connection is closed with a policy-violation
status if the parameter is missing, the token is invalid or expired, or its user no longer exists.
Unlike the REST endpoints, the WebSocket does not check whether the user is inactive: a token
issued before its user was deactivated still opens the console until it expires.

The close reason names why a token was rejected:

| Token | Close reason |
|-------|--------------|
| `token` parameter missing | `token parameter must be specified` |
| Empty | `empty token` |
| Not a JWT | `invalid token` |
| Unsigned JWT | `invalid token format` |
| Signed with a different key | `signature validation failed` |
| Expired | `expired token` |
| Valid, but its user no longer exists | the default policy-violation reason |

Messages are JSON objects identified by a `type` field.

Client to server:

| Message | Effect |
|---------|--------|
| `{"type":"command","command":"<command>"}` | Runs the command on the server console if the server is running |
| `{"type":"start"}` | Starts the server if it is not running |
| `{"type":"ping"}` | Answered with a `pong` message |

A command that is run is recorded in the audit log as `Executed command "<command>"`, and a start as
`Started the server`. A command sent while the server is stopped, or a start sent while it is
running, is ignored and not recorded.

Server to client:

| Message | Meaning |
|---------|---------|
| `{"type":"log","text":"<line>"}` | A line of server console output |
| `{"type":"status"}` | The server has started or stopped; fetch `GET /api/v2/server` for the new state |
| `{"type":"pong"}` | Reply to a `ping` |
