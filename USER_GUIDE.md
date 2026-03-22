# User Guide

## Prerequisites

- Java 17 or later
- Node.js 16 or later
- A PostgreSQL database
- A Minecraft server JAR (e.g. Spigot or Paper)

## First Steps

1. **Set up the database** – Create a PostgreSQL database named `vantage` (or your preferred name).
2. **Configure the backend** – Run the backend once to generate a default `config.json`, then edit it to match your environment (see [Configuration Guide](CONFIG.md)).
3. **Start the backend** – Run `java -jar vantage-<version>-all.jar`. On first launch, an admin account is created automatically and the password is saved to `.admin_password`.
4. **Configure the frontend** – Copy `frontend/.env.local.example` to `frontend/.env.local` and set the API URL and WebSocket URL to match your backend.
5. **Start the frontend** – Run `npm run dev` (development) or `npm run build && npm start` (production) in the `frontend/` directory.
6. **Log in** – Open `http://localhost:3000` in your browser and log in with the admin credentials.

## Common Scenarios

### Managing Users

- Navigate to the **Users** page to view all panel users.
- Click **Create User** to add a new user with a username and password.
- Click on a user to modify their details or status.

### Browsing Server Files

- Navigate to the **Files** page to browse, upload, and delete files in the Minecraft server directory.

### Viewing the Server Console

- Navigate to the **Console** page for a live view of the Minecraft server console via WebSocket.

### Viewing Audit Logs

- Navigate to the **Audit** page to see a paginated history of actions performed by panel users.

## Permissions

Vantage uses a simple authentication model. All authenticated users have access to all panel features. User access is controlled by account status (active or inactive).
