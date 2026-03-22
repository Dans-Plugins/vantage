# Vantage

## Description

Vantage is a web-based management panel for Minecraft servers. It provides a modern dashboard for server administration, including user management, file browsing, live console access via WebSocket, audit logging, and server monitoring. The project consists of a Kotlin backend (HTTP4K + Jetty) and a Next.js (TypeScript) frontend.

## Installation

### Prerequisites

- Java 17 or later
- Node.js 16 or later
- PostgreSQL database

### Backend

1. Clone the repository: `git clone https://github.com/Dans-Plugins/vantage.git`
2. Navigate to the backend directory: `cd vantage/backend`
3. Build the backend: `./gradlew build`
4. Configure `config.json` (see [Configuration Guide](CONFIG.md)).
5. Run the backend: `java -jar build/libs/vantage-*-all.jar`

### Frontend

1. Navigate to the frontend directory: `cd vantage/frontend`
2. Copy the example environment file: `cp .env.local.example .env.local`
3. Configure the environment variables in `.env.local`.
4. Install dependencies: `npm install`
5. Start the development server: `npm run dev`

## Usage

### Documentation

- [User Guide](USER_GUIDE.md) – Getting started and common scenarios
- [API Reference](COMMANDS.md) – Complete list of all API endpoints
- [Configuration Guide](CONFIG.md) – Detailed configuration options

## Support

You can find the support Discord server [here](https://discord.gg/xXtuAQ2).

### Experiencing a bug?

Please fill out a bug report [here](https://github.com/Dans-Plugins/vantage/issues/new).

- [Known Bugs](https://github.com/Dans-Plugins/vantage/issues?q=is%3Aissue+is%3Aopen+label%3Abug)

## Contributing

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Testing

### Unit Tests

Linux:

    cd backend && ./gradlew clean test

Windows:

    cd backend && .\gradlew.bat clean test

If you see `BUILD SUCCESSFUL`, the tests have passed.

## Development

### Backend

1. Configure `config.json` in the backend working directory (see [Configuration Guide](CONFIG.md)).
2. Build the backend: `cd backend && ./gradlew build`
3. Run: `java -jar build/libs/vantage-*-all.jar`

### Frontend

1. Copy `frontend/.env.local.example` to `frontend/.env.local` and configure as needed.
2. Install dependencies: `cd frontend && npm install`
3. Start the development server: `npm run dev`

The frontend will be accessible at `http://localhost:3000`.

## Authors and Acknowledgement

### Developers

| Name | Main Contributions |
|------|--------------------|
| Ren Binden | Creator and lead developer |

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE) (GPL-3.0).

You are free to use, modify, and distribute this software, provided that:

- Source code is made available under the same license when distributed.
- Changes are documented and attributed.
- No additional restrictions are applied.

See the [LICENSE](LICENSE) file for the full text of the GPL-3.0 license.

## Project Status

This project is in active development.
