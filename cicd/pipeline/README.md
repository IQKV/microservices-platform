# CI/CD Pipelines

> **Template reference only.** These Drone CI pipeline files are examples showing the patterns used by this platform. Copy and adapt them for your own services — they are not executed in this repository.

## File Reference

| File                                         | Service type                                                                         | Pipelines |
| -------------------------------------------- | ------------------------------------------------------------------------------------ | --------- |
| `foundation-microservice-project-layout.yml` | **Java service template** — copy this as the starting point for any new microservice | 10        |
| `foundation-iam-service.yml`                 | IAM Service (Java/Maven, with MinIO object storage)                                  | 10        |
| `foundation-billing-service.yml`             | Billing Service (Java/Maven)                                                         | 10        |
| `foundation-audit-service.yml`               | Audit Service (Java/Maven)                                                           | 10        |
| `foundation-gateway-service.yml`             | Gateway Service (Java/Maven)                                                         | 10        |
| `foundation-audit-model.yml`                 | Audit Model library (Java/Maven, no deploy)                                          | 2         |
| `foundation-audit-spi.yml`                   | Audit SPI library (Java/Maven, no deploy)                                            | 2         |
| `foundation-misc-util.yml`                   | Misc utility library (Java/Maven, no deploy)                                         | 2         |
| `foundation-infra.yml`                       | Infrastructure services (Helm only, no build)                                        | 3         |
| `foundation-ui-app.yml`                      | Tenant App (React/pnpm, Nginx container)                                             | 4         |
| `foundation-ui-platform-admin.yml`           | Platform Admin UI (React/pnpm, Nginx container)                                      | 4         |
| `foundation-ui-saas-landing-kit.yml`         | SaaS Landing Kit (Astro/pnpm, Nginx container)                                       | 4         |

## Pipeline Types

### Full Java microservice (10 pipelines)

Used by: `foundation-iam-service`, `foundation-billing-service`, `foundation-audit-service`, `foundation-gateway-service`, and the project layout template.

| #   | Pipeline                    | Event            | Trigger ref                                                                                             | What it does                                                                                                                                                         |
| --- | --------------------------- | ---------------- | ------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `VerifyCode`                | push, tag        | dev, feature/_, rfc/_, poc/_, bugfix/_, improvement/_, prerelease/_, library/_, hotfix/_, `*.*.x`, tags | `mvn clean verify` (tests + coverage) → SonarQube → PMD → SpotBugs. Fails fast on any gate.                                                                          |
| 2   | `PublishArtifacts`          | push, tag        | dev, prerelease/_, `_.\*.x`, tags                                                                       | `mvn deploy` SNAPSHOT to Nexus on branches; release JAR on tags. On tags: creates GitHub Release via `release-it`.                                                   |
| 3   | `PublishDockerImage`        | push, tag        | wip, feature/\*, tags                                                                                   | Packages JAR, downloads `JDK.alpine-simple.dockerfile`, builds and pushes Docker image. Tag strategy: `wip`→branch name, `feature/*`→stripped name, tags→semver tag. |
| 4   | `DeployWorkInProgress`      | push             | wip                                                                                                     | `helm upgrade --install --atomic` to `iqkv-sit-env`. Depends on `PublishDockerImage`.                                                                                |
| 5   | `RollbackWorkInProgress`    | rollback→sit     | wip                                                                                                     | `helm uninstall` from `iqkv-sit-env`.                                                                                                                                |
| 6   | `PromoteFeatureDeployment`  | promote→sit      | feature/\*                                                                                              | Helm deploy to `iqkv-sit-env` with feature image tag.                                                                                                                |
| 7   | `RollbackFeatureDeployment` | rollback→sit     | feature/\*                                                                                              | `helm uninstall` from `iqkv-sit-env`.                                                                                                                                |
| 8   | `PromoteDeployment`         | promote→uat/prd  | tags                                                                                                    | Helm deploy to `iqkv-uat-env` or `iqkv-prd-env` using semver tag.                                                                                                    |
| 9   | `RollbackDeployment`        | rollback→uat/prd | tags                                                                                                    | `helm uninstall` from UAT/PRD namespace.                                                                                                                             |
| 10  | `ReleasePackage`            | promote→release  | dev, `*.*.x`                                                                                            | Strips `-SNAPSHOT`, creates git tag, pushes, bumps pom + package.json to next SNAPSHOT, updates CHANGELOG.                                                           |

**Static analysis detail:**

- Primary services (`iam`, `billing`, `audit`, `gateway`): SonarQube `5.6.0.6792` + PMD (High priority, custom ruleset) + SpotBugs `4.9.8.3`
- Library modules (`audit-model`, `audit-spi`): SonarQube `3.11.0.3922` + SpotBugs only

**IAM service differences:** Deploy steps additionally pass `infraServices.objectstorage.accessKey` and `infraServices.objectstorage.secretKey` to Helm for MinIO connectivity.

### Library module (2 pipelines)

Used by: `foundation-audit-model`, `foundation-audit-spi`, `foundation-misc-util`.

`VerifyCode` + `PublishArtifacts` only. No Docker image, no deployment.

### Infrastructure (3 pipelines)

`foundation-infra.yml` manages shared Kubernetes infrastructure (PostgreSQL, Redis, RabbitMQ, MinIO, MailHog, DbGate).

| #   | Pipeline                 | Event                | What it does                                                                                                                                                |
| --- | ------------------------ | -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 0   | `Info`                   | push to dev          | Clones Helm charts repo, runs `helm template` for dry-run validation                                                                                        |
| 1   | `PromoteInfrastructure`  | promote→sit/uat/prd  | Helm deploy with full verification: `kubectl wait` for pods, psql connectivity checks for each DB, cleanup-on-failure step that uninstalls and deletes PVCs |
| 2   | `RollbackInfrastructure` | rollback→sit/uat/prd | Deep cleanup: `helm uninstall` + force-delete all PVCs                                                                                                      |

### Frontend (4 pipelines)

Used by: `foundation-ui-app`, `foundation-ui-platform-admin`, `foundation-ui-saas-landing-kit`.

| #   | Pipeline                 | What it does                                                                    |
| --- | ------------------------ | ------------------------------------------------------------------------------- |
| 1   | `VerifyCode`             | pnpm formatter:check + lint + test:coverage + SonarQube + build                 |
| 2   | `PublishArtifacts`       | `pnpm publish` to private Nexus NPM registry                                    |
| 3   | `DeployWorkInProgress`   | Downloads Nginx Dockerfile, `pnpm build`, Docker image push, Helm deploy to SIT |
| 4   | `RollbackWorkInProgress` | `helm uninstall` from SIT                                                       |

## Adapting for a New Service

1. Copy `foundation-microservice-project-layout.yml` to your service's repository.
2. Replace all Helm `--set` values with those matching your service's `values.yaml`.
3. Add any service-specific secrets (e.g. Stripe keys, S3 credentials) to the deploy steps.
4. Configure secrets in Drone UI at repository level.
5. Ensure your Helm chart is present in the `HELM_CHARTS_REPOSITORY` under `{owner}/{repo-name}/`.

## Caching

All pipelines use host-mounted volumes for dependency caching:

| Volume name   | Host path          | Container path      | Used by                           |
| ------------- | ------------------ | ------------------- | --------------------------------- |
| `maven-cache` | `/app/.m2`         | `/root/.m2`         | Java pipelines                    |
| `pnpm-store`  | `/app/.pnpm-store` | `/root/.pnpm-store` | All pipelines (pnpm lint/publish) |
| `npm-cache`   | `/app/.npm-cache`  | `/root/.npm`        | Static analysis step              |
