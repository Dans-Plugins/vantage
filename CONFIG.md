# Configuration Guide

Vantage uses a `config.json` file in the backend working directory. If the file does not exist on first launch, a default configuration is generated automatically.

## server

### server.name

**Type:** string
**Default:** `"Server"`
**Description:** The display name of the server shown in the panel.

### server.port

**Type:** integer
**Default:** `9000`
**Description:** The port the backend HTTP server listens on.

### server.baseUrl

**Type:** string
**Default:** `""`
**Description:** The base URL of the backend API. Used for generating URLs in API responses.

### server.allowedOrigins

**Type:** list of strings
**Default:** `["http://localhost:3000"]`
**Description:** List of origins allowed to make cross-origin requests (CORS). Add the URL of your frontend deployment here.

## database

### database.url

**Type:** string
**Default:** `"jdbc:postgresql://localhost/vantage"`
**Description:** The JDBC connection URL for the database.

### database.username

**Type:** string (nullable)
**Default:** `null`
**Description:** The username for the database connection. Leave unset if your database does not require authentication.

### database.password

**Type:** string (nullable)
**Default:** `null`
**Description:** The password for the database connection. Leave unset if your database does not require authentication.

### database.dialect

**Type:** string
**Default:** `"POSTGRES"`
**Description:** The jOOQ SQL dialect to use. Must match your database type (e.g. `POSTGRES`, `MYSQL`, `H2`).

## minecraft

### minecraft.command

**Type:** string
**Default:** `"java -jar spigot-1.19.2.jar -nogui"`
**Description:** The command used to start the Minecraft server process.

### minecraft.directory

**Type:** string
**Default:** `"./minecraft-server"`
**Description:** The working directory for the Minecraft server. This is also the root directory for the file browser.

## Example

```json
{
  "server": {
    "name": "My Server",
    "port": 9000,
    "baseUrl": "https://panel.example.com",
    "allowedOrigins": [
      "https://panel.example.com"
    ]
  },
  "database": {
    "url": "jdbc:postgresql://localhost/vantage",
    "username": "vantage",
    "password": "secret",
    "dialect": "POSTGRES"
  },
  "minecraft": {
    "command": "java -jar paper-1.19.2.jar -nogui",
    "directory": "/opt/minecraft"
  }
}
```

## Frontend Environment Variables

The frontend uses environment variables configured in `frontend/.env.local`. Copy `frontend/.env.local.example` to get started.

| Variable | Description | Example |
|----------|-------------|---------|
| `NEXT_PUBLIC_VANTAGE_API_URL` | Backend API URL (client-side) | `http://localhost:9000/api/v2` |
| `VANTAGE_API_URL` | Backend API URL (server-side) | `http://localhost:9000/api/v2` |
| `NEXT_PUBLIC_VANTAGE_WEBSOCKET_URL` | WebSocket URL for live console | `ws://localhost:9000/ws` |
| `NEXTAUTH_SECRET` | Secret for NextAuth.js session signing | Generate with `openssl rand -base64 32` |
| `NEXTAUTH_URL` | The canonical URL of the frontend | `http://localhost:3000` |
