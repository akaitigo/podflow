.PHONY: proto-lint frontend-check backend-check check clean

proto-lint:
	cd proto && buf lint && buf format --diff

frontend-check:
	cd frontend && pnpm run check

backend-check:
	cd backend && ./gradlew build

check: proto-lint frontend-check backend-check
	@echo "All checks passed."

clean:
	rm -rf frontend/dist frontend/coverage
	cd backend && ./gradlew clean
