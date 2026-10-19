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
- Characterization tests for the file-management handler (`FilesHandler`) covering directory
  listing, file reads, writes, and deletes, including the current handling of `..` paths.
- Characterization tests for authentication (`Authenticator`, the `Authenticated` filter, and
  `Request.userId`) covering token issue and expiry, rejected credentials and inactive users,
  signature verification, and how the filter treats missing, malformed, and stale tokens.
- Characterization tests for the login handler (`LoginHandler`) covering the response body, the
  "Logged in" audit entry, and rejected logins, and for the brand handler (`BrandHandler`).
- Characterization tests for the user handlers (`UserHandler` and `UsersHandler`) covering user
  lookup, creation, and updates, their audit entries, optimistic locking, and the 50-user list page.
- Documented the request and response bodies of the user endpoints in `COMMANDS.md`.

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
- The list of backend classes covered by tests is now kept only in the Testing section of
  `README.md`; `CONTRIBUTING.md` and `.github/copilot-instructions.md` link to it instead of
  repeating it.
- `CONFIG.md`, `USER_GUIDE.md`, and `COMMANDS.md` now document the `private.pem` token-signing key
  the backend generates in its working directory, the `admin` username of the generated account,
  the login response fields and 24-hour token lifetime, and the `401 Unauthorized` response for
  unauthenticated requests.

### Fixed

- `COMMANDS.md` said the `/ws/log` WebSocket token is sent as a message after connecting; it is a
  `token` query parameter on the connection URL. The section now also documents the message format.
- `CONFIG.md` listed `MYSQL` and `H2` as example `database.dialect` values; the JAR bundles only the
  PostgreSQL driver and the schema uses PostgreSQL column types, so only `POSTGRES` works.

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
