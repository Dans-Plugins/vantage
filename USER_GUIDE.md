# User Guide

## Prerequisites

- Java 17 or later
- Node.js 16 or later
- A PostgreSQL database
- A Minecraft server JAR (e.g. Spigot or Paper)

## First Steps

1. **Set up the database** – Create a PostgreSQL database named `vantage` (or your preferred name).
2. **Configure the backend** – Run the backend once to generate a default `config.json`, then edit it to match your environment (see [Configuration Guide](CONFIG.md)). The file is written before the backend connects to the database, so this first run ends with a connection error if the defaults do not reach your database.
3. **Add the Minecraft server** – Put your server JAR in the directory set by `minecraft.directory` (`./minecraft-server` by default, relative to the backend's working directory), and make sure `minecraft.command` names that JAR.
4. **Start the backend** – Run `java -jar vantage-<version>-all.jar`. The backend starts the Minecraft server as well. On first launch (when the database has no panel users), an account named `admin` is created automatically and its password is saved to `.admin_password` in the working directory. The backend also generates `private.pem`, the key that signs login tokens (see [Generated Files](CONFIG.md#generated-files)).
5. **Configure the frontend** – Copy `frontend/.env.local.example` to `frontend/.env.local`, set the API URL and WebSocket URL to match your backend, and set `NEXTAUTH_SECRET` (generate a value with `openssl rand -base64 32`).
6. **Start the frontend** – Run `npm run dev` (development) or `npm run build && npm start` (production) in the `frontend/` directory.
7. **Log in** – Open `http://localhost:3000` in your browser and log in with the username `admin` and the password from `.admin_password`.

## Common Scenarios

The navigation bar links to the **Home**, **Console**, **Files**, **Audit**, and **Users** pages, and its **Logout** button ends the session. The **Home** page shows the username you are logged in as.

### Managing Users

- Navigate to the **Users** page to view panel users, with their ID, username, and status.
- Click **Create** to add a new user. Enter a username and password, then click **Save**.
- Click **Edit** in a user's row to change their username, password, or status (Active or Inactive), then click **Save**. Leave the password field empty to keep the current password.

### Browsing Server Files

- Navigate to the **Files** page to browse the Minecraft server directory. Click a folder to open it, or **..** to go back up.
- Click a file to download it.
- Click **Upload** to choose a single file to upload.
- Click the delete icon next to a file or folder to delete it. There is no confirmation, and deleting a folder also deletes everything inside it.

### Viewing the Server Console

- Navigate to the **Console** page for a live view of the Minecraft server console via WebSocket.
- The Minecraft server starts with the backend, so it is normally already running when you open this page.
- Click **Start** to start the server, or **Stop** to stop it. Only the button that applies to the server's current state is shown. **Stop** sends the `stop` command to the server.
- Type a command in the **Command** field and press Enter or click **Send** to run it on the server.
- If the server exits straight away and the console stays empty (for example, because the JAR is missing), check the backend's own output: errors the server writes to standard error do not reach the console, and the backend logs `Minecraft server exited with code <n>.`
- Stop the server before shutting down the backend. The backend waits for the server to exit, and the `stop` it sends on shutdown does not reach the server, so a running server can keep the backend from exiting.

### Viewing Audit Logs

- Navigate to the **Audit** page to see a paginated history of actions performed by panel users, showing the user, a description, and the time of each action. Logins, user changes, console commands, and server starts from the panel are recorded. File uploads and deletes are not, and neither is the server start that happens with the backend.

## Permissions

Vantage uses a simple authentication model. All authenticated users have access to all panel features. User access is controlled by account status (active or inactive).
