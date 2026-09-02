# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- JUnit 5 test suite for the backend, with a `org.junit.jupiter:junit-jupiter` test dependency in
  `backend/build.gradle` and a test source set at `backend/src/test/kotlin/`.
- Characterization tests for the user domain model (`User` and `UserId`) covering password hashing
  and verification, `withPassword`, `copy`, and equality.

### Changed

- Corrected the testing documentation in `README.md`, `CONTRIBUTING.md`, and
  `.github/copilot-instructions.md`, which now describes the JUnit 5 suite, where tests live, and
  which parts of the backend remain uncovered.
- Documented `./gradlew clean build` as the automated check that CI actually runs, and noted that
  the frontend has no test tooling or CI coverage.
- Replaced the `create-next-app` boilerplate in `frontend/README.md` with documentation of the
  frontend's actual setup, scripts, environment variables, and project structure.

## [2.0.1] – 2023-01-01

### Added

- Initial public release of the Vantage management panel.
- Kotlin backend with HTTP4K and Jetty.
- Next.js frontend with TypeScript.
- User authentication with JWT.
- User management (create, update, list).
- Server file browser (list, upload, delete).
- Live server console via WebSocket.
- Audit logging of user actions.
- PostgreSQL database support with Flyway migrations.
- Server branding endpoint.
