# Vantage Frontend

The Vantage web panel: a [Next.js](https://nextjs.org/) application written in TypeScript, using MUI
for the interface, NextAuth.js for session handling, and SWR for data fetching. It talks to the
Vantage backend over the REST API and WebSocket endpoint documented in the
[API Reference](../COMMANDS.md).

## Getting Started

1. Copy the example environment file: `cp .env.local.example .env.local`
2. Configure the variables in `.env.local` (see [Environment Variables](#environment-variables)).
3. Install dependencies: `npm install`
4. Start the development server: `npm run dev`

Open [http://localhost:3000](http://localhost:3000) in your browser. A running Vantage backend is
required — without it, the panel cannot log in or display any data.

## Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start the development server on port 3000. |
| `npm run build` | Produce a production build. |
| `npm start` | Serve a production build (run `npm run build` first). |
| `npm run lint` | Run ESLint via `next lint`. |

There is no test script: the frontend has no test tooling, and no CI job builds, lints, or tests it.
Run `npm run lint` and `npm run build` locally, and exercise changes in a browser.

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `NEXT_PUBLIC_VANTAGE_API_URL` | Backend API URL (client-side) | `http://localhost:9000/api/v2` |
| `VANTAGE_API_URL` | Backend API URL (server-side; must be reachable from the Next.js server, which performs authentication) | `http://localhost:9000/api/v2` |
| `NEXT_PUBLIC_VANTAGE_WEBSOCKET_URL` | WebSocket URL for the live console | `ws://localhost:9000/ws` |
| `NEXTAUTH_SECRET` | Secret for NextAuth.js session signing | Generate with `openssl rand -base64 32` |
| `NEXTAUTH_URL` | The canonical URL of the frontend | `http://localhost:3000` |

The backend must list the frontend's origin in `server.allowedOrigins` for these requests to pass
CORS. See the [Configuration Guide](../CONFIG.md) for the backend side of this configuration.

## Project Structure

- `pages/` – Next.js routes (`index`, `users`, `files`, `console`, `audit`), plus the NextAuth route
  under `pages/api/auth/`.
- `components/<Name>/` – One directory per React component, containing `<Name>.tsx`, an `index.ts`
  barrel export, and an optional `<Name>.module.css`.
- `hooks/` – SWR-based data-fetching hooks, one `use<Thing>.ts` per backend resource.
- `src/` – Shared utilities, including `fetchWithToken` and `mutateWithToken`.
- `types/` – Shared TypeScript type definitions.
- `styles/` – Global CSS.
- `middleware.ts` – NextAuth middleware requiring an authenticated session.

## Learn More

- [Next.js Documentation](https://nextjs.org/docs)
- [NextAuth.js Documentation](https://next-auth.js.org/)
- [MUI Documentation](https://mui.com/material-ui/getting-started/)
