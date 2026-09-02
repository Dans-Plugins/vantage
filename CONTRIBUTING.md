# Contributing

## Thank You

Thank you for your interest in contributing to Vantage! This guide will help you get started.

## Links

- [Website](https://dansplugins.com)
- [Discord](https://discord.gg/xXtuAQ2)

## Requirements

- A GitHub account
- Git installed on your local machine
- A Java IDE or text editor
- A basic understanding of Kotlin and/or TypeScript
- Java 17 or later (for the backend)
- Node.js 16 or later (for the frontend)

## Getting Started

1. [Sign up for GitHub](https://github.com/signup) if you don't have an account.
2. Fork the repository by clicking **Fork** at the top right of the repo page.
3. Clone your fork: `git clone https://github.com/<your-username>/vantage.git`
4. Open the project in your IDE.
5. Build the backend: `cd backend && ./gradlew build`
6. Install frontend dependencies: `cd frontend && npm install`
   If you encounter errors, please open an issue.

## Identifying What to Work On

### Issues

Work items are tracked as [GitHub issues](https://github.com/Dans-Plugins/vantage/issues).

### Milestones

Issues are grouped into [milestones](https://github.com/Dans-Plugins/vantage/milestones) representing upcoming releases.

## Making Changes

1. Make sure an issue exists for the work. If not, create one.
2. Switch to `develop`: `git checkout develop`
3. Create a branch: `git checkout -b <branch-name>`
4. Make your changes.
5. Test your changes.
6. Commit: `git commit -m "Description of changes"`
7. Push: `git push origin <branch-name>`
8. Open a pull request against `develop`, link the related issue with `#<number>`.
9. Address review feedback.

## Testing

The backend is tested with JUnit 5. Tests live under
`backend/src/test/kotlin/uk/co/renbinden/vantage/`, mirroring the main package structure, in files
named `<ClassUnderTest>Test.kt`. Coverage is currently limited to the user domain model (`User` and
`UserId`); most of the backend is still untested, so a green test task is not evidence that an
untested area works.

Verify changes with the full build — this is the command CI runs, and it includes the test task:

Linux: `cd backend && ./gradlew clean build`
Windows: `cd backend && .\gradlew.bat clean build`

A successful run confirms that jOOQ code generation succeeds, the Kotlin sources compile, the test
suite passes, and the shadow JAR assembles. It does not verify runtime behaviour outside the
covered classes, so test your changes manually as well.

The frontend has no test tooling and is not covered by CI. Check frontend changes with
`cd frontend && npm run lint` and `cd frontend && npm run build`, and exercise them in a browser.

Tests that need a database, a Minecraft process, or the network are out of scope for this suite —
prefer temporary directories and test doubles over real collaborators.

## Questions

Ask in the [Discord server](https://discord.gg/xXtuAQ2).
