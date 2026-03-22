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
- Test framework: JUnit 5

## Project Structure

- `backend/src/main/kotlin/` – Backend source code (Kotlin)
- `backend/src/main/resources/` – Database migration files
- `backend/src/test/java/` – Backend unit tests
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

- Branch from `develop` for all changes.
- Open a pull request against `develop`, not `main`.
- Reference the related GitHub issue in every pull request description.
