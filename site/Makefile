.DEFAULT_GOAL := help

PNPM := pnpm

# ── Dependencies ──────────────────────────────────────────────────────────────

.PHONY: install
install: ## Install dependencies
	$(PNPM) install --frozen-lockfile

# ── Development ───────────────────────────────────────────────────────────────

.PHONY: dev
dev: ## Start the local dev server (http://localhost:4321/dmx-fun/)
	$(PNPM) run dev

.PHONY: preview
preview: ## Preview the production build locally
	$(PNPM) run preview

# ── Build ─────────────────────────────────────────────────────────────────────

.PHONY: build
build: ## Build the production site to ./dist/
	$(PNPM) run build

.PHONY: build-pages
build-pages: ## Build for GitHub Pages (sets GITHUB_PAGES=true)
	GITHUB_PAGES=true $(PNPM) run build

# ── Javadocs ──────────────────────────────────────────────────────────────────

.PHONY: copy-javadocs
copy-javadocs: ## Copy Javadoc output from Gradle into public/
	$(PNPM) run copy:javadocs

# ── Docker ────────────────────────────────────────────────────────────────────

.PHONY: docker-build
docker-build: build ## Build production site and create Docker image
	docker build -t dmx-fun-site .

.PHONY: docker-run
docker-run: ## Run the Docker image locally on port 8080
	docker run --rm -p 8080:80 dmx-fun-site

# ── Astro CLI ─────────────────────────────────────────────────────────────────

.PHONY: astro-check
astro-check: ## Run astro type-checking
	$(PNPM) run astro check

.PHONY: astro-sync
astro-sync: ## Sync Astro content collection types
	$(PNPM) run astro sync

# ── Cleanup ───────────────────────────────────────────────────────────────────

.PHONY: clean
clean: ## Remove build output
	rm -rf dist

.PHONY: clean-all
clean-all: clean ## Remove build output and node_modules
	rm -rf node_modules

# ── Help ──────────────────────────────────────────────────────────────────────

.PHONY: help
help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
