# User Guide

## Prerequisites

- Java 17 or later
- Node.js 16 or later
- A PostgreSQL database
- A Minecraft server JAR (e.g. Spigot or Paper)

## First Steps

1. **Set up the database** – Create a PostgreSQL database named `vantage` (or your preferred name).
2. **Configure the backend** – Run the backend once to generate a default `config.json`, then edit it to match your environment (see [Configuration Guide](CONFIG.md)).
3. **Start the backend** – Run `java -jar vantage-<version>-all.jar`. On first launch (when the database has no panel users), an account named `admin` is created automatically and its password is saved to `.admin_password` in the working directory. The backend also generates `private.pem`, the key that signs login tokens (see [Generated Files](CONFIG.md#generated-files)).
4. **Configure the frontend** – Copy `frontend/.env.local.example` to `frontend/.env.local` and set the API URL and WebSocket URL to match your backend.
5. **Start the frontend** – Run `npm run dev` (development) or `npm run build && npm start` (production) in the `frontend/` directory.
6. **Log in** – Open `http://localhost:3000` in your browser and log in with the username `admin` and the password from `.admin_password`.

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
- Click **Start** to start the server, or **Stop** to stop it. Only the button that applies to the server's current state is shown.
- Type a command in the **Command** field and press Enter or click **Send** to run it on the server.

### Viewing Audit Logs

- Navigate to the **Audit** page to see a paginated history of actions performed by panel users, showing the user, a description, and the time of each action.

## Permissions

Vantage uses a simple authentication model. All authenticated users have access to all panel features. User access is controlled by account status (active or inactive).
