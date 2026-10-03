# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- JUnit 5 test suite for the backend, with a `org.junit.jupiter:junit-jupiter` test dependency in
  `backend/build.gradle` and a test source set at `backend/src/test/kotlin/`.
- Characterization tests for the user domain model (`User` and `UserId`) covering password hashing
  and verification, `withPassword`, `copy`, and equality.
- Characterization tests for the console WebSocket message format (`MessageGson`) covering every
  client-to-server and server-to-client message type.

### Changed

- Corrected the testing documentation in `README.md`, `CONTRIBUTING.md`, and
  `.github/copilot-instructions.md`, which now describes the JUnit 5 suite, where tests live, and
  which parts of the backend remain uncovered.
- Documented `./gradlew clean build` as the automated check that CI actually runs, and noted that
  the frontend has no test tooling or CI coverage.
- Replaced the `create-next-app` boilerplate in `frontend/README.md` with documentation of the
  frontend's actual setup, scripts, environment variables, and project structure.
- `CONTRIBUTING.md`, `.github/copilot-instructions.md`, and the `Build` workflow now name `main` as
  the branch to work from and open pull requests against; the `develop` instructions they carried
  were template wording that did not match how the repository is integrated.
- The `Release` workflow now runs when a release is published (rather than created, which never
  fired for drafts) and skips the rebuild when the release already has a JAR attached, so the
  verified JAR published by the release automation is the only one on the release.

### Fixed

- `COMMANDS.md` said the `/ws/log` WebSocket token is sent as a message after connecting; it is a
  `token` query parameter on the connection URL. The section now also documents the message format.

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
