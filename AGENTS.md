# Repository Guidelines

## Project Structure & Module Organization
`backend/` contains the Spring Boot API. Java code lives under `backend/src/main/java/com/daifu/manage`, grouped by domain (`auth`, `user`, `store`, `product`, `cart`, `payment`, `stats`) with `controller`, `service`, `mapper`, `dto`, `entity`, and `vo` packages. Config and SQL files live in `backend/src/main/resources`, including `application.yml` and `db/migration`. Tests are under `backend/src/test/java`.

`frontend/admin-web/` contains the Vue 3 admin app. App entry files are in `src/`, with API wrappers in `src/api`, routing in `src/router`, state in `src/stores`, and page components in `src/views`. Treat `dist/`, `target/`, and `node_modules/` as generated output.

## Build, Test, and Development Commands
- `cd backend && mvn spring-boot:run`: start the backend on port `8080`.
- `cd backend && mvn test`: run the Spring Boot test suite.
- `cd backend && mvn -q -DskipTests compile`: fast compile check for backend changes.
- `cd frontend/admin-web && npm run dev`: start the Vite dev server with `/api` proxied to the backend.
- `cd frontend/admin-web && npm run build`: produce a production bundle in `frontend/admin-web/dist`.
- `cd frontend/admin-web && npm run preview`: preview the built frontend locally.

## Coding Style & Naming Conventions
Follow existing conventions instead of introducing new patterns. Java uses 4-space indentation, `UpperCamelCase` class names, `lowerCamelCase` fields/methods, and domain packages under `com.daifu.manage`. Vue and JavaScript files use 2-space indentation, double quotes, `PascalCase` for view components (`UserView.vue`), and lower-case API/store modules (`user.js`, `auth.js`). No formatter or linter config is checked in, so keep diffs small and style-consistent.

## Testing Guidelines
Backend tests use JUnit 5 via `spring-boot-starter-test`. Name test classes `*Tests` and place them under the matching package in `backend/src/test/java`. Add at least one automated test for new backend logic when practical. The frontend currently has no test runner configured, so verify UI changes with `npm run build` and a manual smoke test of the affected pages.

## Commit & Pull Request Guidelines
Recent history uses short Chinese summaries for single-purpose changes, such as "frontend optimization", "project refactor", and "update". Keep commit subjects brief, action-oriented, and focused on one change. Pull requests should include: a short summary, impacted modules (`backend`, `frontend/admin-web`, or both), setup or migration notes for API/database changes, and screenshots for UI updates.

## Security & Configuration Tips
Do not commit real MySQL or WeChat Pay secrets. Keep local values in `backend/src/main/resources/application.yml` private and sanitize examples in documentation. When changing schema, update the SQL under `backend/src/main/resources/db/migration` and note the migration in the PR.
