# Changelog

## v1.0.0 (2026-04-01)

### Initial Release

- fix: allow clearing guest association via empty guest_id in update mask (#36)
- fix: health check returns 503 on DB failure, block SSRF, fix field clearing (#34)
- fix(build): revert Kotlin 2.3.20 and Quarkus 3.34.1 to 2.1.0/3.17.0
- fix: reduce throw count in validateAudioUrl to satisfy detekt ThrowsCount rule
- fix: address 3-model review findings
- chore(deps): bump pnpm/action-setup from 4 to 5
- chore(deps): bump actions/setup-java from 4 to 5
- chore(deps): bump actions/checkout from 4 to 6
- chore(deps): bump actions/setup-node from 4 to 6
- build(deps): bump plugin.allopen from 2.1.0 to 2.3.20 in /backend (#24)
- build(deps): bump jvm from 2.1.0 to 2.3.20 in /backend (#29)
- build(deps): bump io.quarkus from 3.17.0 to 3.34.1 in /backend (#25)
- build(deps-dev): bump oxlint from 0.12.0 to 1.57.0 in /frontend (#22)
- build(deps-dev): bump @vitejs/plugin-react in /frontend (#20)
- build(deps): bump io.gitlab.arturbosch.detekt in /backend (#26)
- ci: add Dependabot auto-merge workflow
- fix: address review v2 findings
- fix(ci): scope buf breaking --against to proto/ subdir
- fix(ci): fetch origin/main before buf breaking check
- fix: address review findings — security, quality, and tech debt (#11, #12, #13)
- docs: Stage 6 Harvest — template evaluation and retrospective
- docs: replace ASCII art demo with real screenshots
- feat(#5): add E2E tests, ADR-003, and finalize README for ship readiness (#10)
- feat(#4): implement episode kanban board UI with drag-and-drop (#9)
- feat(#3): implement Episode CRUD gRPC service with status transition validation (#8)
- feat(#2): add Episode + Guest data model, gRPC schema, and Flyway migration (#7)
- feat #1: project foundation — CI/CD, lint, test framework (#6)
- docs: add use cases, screen wireframes, and architecture diagrams
- Initial project scaffold from idea #576