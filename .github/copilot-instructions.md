# Copilot Instructions

This repository follows the DPC (Dans Plugins Community) conventions defined at
https://github.com/Dans-Plugins/dpc-conventions. Read those conventions before
making any changes.

## Technology Stack

- Language: Kotlin (backend), TypeScript (frontend)
- Build tool: Gradle (Groovy DSL) for backend, npm for frontend
- Backend framework: HTTP4K with Jetty
- Frontend framework: Next.js (React)
- Database: PostgreSQL with jOOQ and Flyway migrations
- Authentication: JWT (jjwt) on the backend, NextAuth.js on the frontend
- Test framework: JUnit 5 (`org.junit.jupiter:junit-jupiter`, declared in `backend/build.gradle`)
  for the backend; the frontend has none. Backend coverage is limited to the user domain model
  and the WebSocket message format — see the Testing section of `README.md`.

## Project Structure

- `backend/src/main/kotlin/` – Backend source code (Kotlin)
- `backend/src/main/resources/` – Database migration files
- `backend/src/test/kotlin/` – Backend unit tests, mirroring the main package structure
- `frontend/pages/` – Next.js pages and API routes
- `frontend/components/` – React components
- `frontend/hooks/` – Custom React hooks
- `frontend/src/` – Frontend utility functions
- `frontend/styles/` – CSS styles
- `frontend/types/` – TypeScript type definitions

## Coding Conventions

- Follow the existing package structure (`uk.co.renbinden.vantage`) when adding new Kotlin classes.
- Use data classes for request/response models.
- All API endpoints are defined in `endpoint/api/v2/` with handler classes.
- Use the `Authenticated` filter for endpoints that require authentication.
- Frontend components each have their own directory under `components/` with an `index.ts` barrel export.

## Contribution Workflow

- Branch from `main` for all changes; `main` is the integration branch and the only branch the
  `Build` workflow runs on.
- Open a pull request against `main`.
- Reference the related GitHub issue in the pull request description when one exists.
- See the Making Changes section of `CONTRIBUTING.md` for the full workflow.
