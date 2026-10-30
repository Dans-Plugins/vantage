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
- Characterization tests for the audit handler (`AuditHandler`) covering newest-first ordering,
  the 50-item page, non-numeric pages, and the user embedded in each item.
- Documented the response body of `GET /api/v2/audit/{page}` in `COMMANDS.md`.
- Characterization tests for the server handlers (`ServerHandler` and `ServerLogHandler`) covering
  the running state and the full, untrimmed console log.
- Documented the response bodies of `GET /api/v2/server` and `GET /api/v2/server/log` in
  `COMMANDS.md`.
- Characterization tests for the console WebSocket handler (`ConsoleHandler`) covering each token
  rejection and its close reason, command and start handling, audit entries, and ping replies.
- Documented in `COMMANDS.md` that `/ws/log` accepts tokens of inactive users, which close reason
  each rejected token receives, and which console actions are audited.
- Characterization tests for the configuration model (`Config` and its sections) covering the
  defaults, the generated `config.json`, and how omitted, unknown, and `null` keys are read.
- Documented in `CONFIG.md` that keys omitted from `config.json` take their defaults and that the
  generated file leaves out `database.username` and `database.password`.
- Documented the file endpoints in `COMMANDS.md`: how nested paths are addressed, the listing and
  upload responses, recursive deletes, and the current handling of `..` paths, with tests covering
  nested reads and writes and uploads into a missing directory.
- Characterization tests for the Minecraft server process wrapper (`MinecraftServer`), run against a
  stand-in Java program, covering the captured standard output (standard error is not captured),
  the working directory, whitespace splitting of the command, the running state, and the messages
  sent to console sockets.

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
- `USER_GUIDE.md` now names the panel's actual controls (**Create** and **Edit** on the Users page,
  rather than **Create User** and clicking a user) and describes the Files, Console, Audit, and
  Home pages as the frontend implements them, including that deletes are immediate and recursive.

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
