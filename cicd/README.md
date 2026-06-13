# CI/CD — Reference Pipelines & Helm Charts

> **Template reference only.** These files illustrate the CI/CD patterns used by the platform. They are not wired to a live CI runner and contain placeholder secrets. Adapt them for your own Drone CI instance.

## Contents

| Folder                            | Description                                     |
| --------------------------------- | ----------------------------------------------- |
| [`pipeline/`](pipeline/README.md) | Drone CI pipeline definitions for every service |
| [`chart/`](chart/README.md)       | Helm charts for Kubernetes deployment           |

## CI/CD Platform

All pipelines run on **[Drone CI](https://www.drone.io/)** with Docker executors (`type: docker`). The custom runner image `cicdtools/pipeline-runner` bundles Java 25, Maven, pnpm, kubectl, Helm, and release tooling.

External dependencies:

| Dependency                                               | Purpose                                                 |
| -------------------------------------------------------- | ------------------------------------------------------- |
| Nexus (`know-how.nexus`)                                 | Maven SNAPSHOT/release repository, private NPM registry |
| Container registry (`know-how.download`)                 | Docker image storage                                    |
| SonarQube                                                | Static analysis quality gate                            |
| Helm charts repository (`HELM_CHARTS_REPOSITORY` secret) | Separate Git repo with per-service Helm values          |
| Slack                                                    | Build and deployment notifications to `#dev` channel    |

## Environments

| Namespace      | Branch / Trigger                       | Purpose                    |
| -------------- | -------------------------------------- | -------------------------- |
| `iqkv-sit-env` | `wip` auto-deploy, `feature/*` promote | System Integration Testing |
| `iqkv-uat-env` | release tag promote                    | User Acceptance Testing    |
| `iqkv-prd-env` | release tag promote                    | Production                 |

## Pipeline Overview

```
push to dev/feature/*      ──► VerifyCode ──► PublishArtifacts ──► (SNAPSHOT to Nexus)
push to wip                ──► VerifyCode ──► PublishDockerImage ──► DeployWorkInProgress (SIT)
tag                        ──► VerifyCode ──► PublishArtifacts ──► PublishDockerImage
promote to sit             ──► PromoteFeatureDeployment (SIT)
promote to uat/prd         ──► PromoteDeployment (UAT/PRD)
promote to release         ──► ReleasePackage (creates tag, bumps SNAPSHOT)
rollback                   ──► Rollback* (helm uninstall)
```

## Required Drone Secrets

All secrets are configured at the Drone repository level.

| Secret                                                                | Used by                                  |
| --------------------------------------------------------------------- | ---------------------------------------- |
| `NEXUS_DEPLOYER_USERNAME` / `NEXUS_DEPLOYER_PASSWORD`                 | Maven deploy, dependency resolution      |
| `NEXUS_NPM_USERNAME` / `NEXUS_NPM_PASSWORD` / `NEXUS_NPM_EMAIL`       | NPM publish                              |
| `SONAR_HOST` / `SONAR_TOKEN`                                          | SonarQube analysis                       |
| `SVC_CONTAINER_REGISTRY_USERNAME` / `SVC_CONTAINER_REGISTRY_PASSWORD` | Docker image push                        |
| `HELM_CHARTS_REPOSITORY`                                              | Git URL of the Helm values repository    |
| `INFRA_POSTGRESQL_PASSWORD`                                           | Helm deploy — DB credentials             |
| `INFRA_RABBITMQ_PASSWORD`                                             | Helm deploy — RabbitMQ credentials       |
| `INFRA_REDIS_PASSWORD`                                                | Infra chart — Redis credentials          |
| `INFRA_S3_ACCESS_KEY` / `INFRA_S3_SECRET_KEY`                         | Helm deploy — MinIO credentials          |
| `SMTP_USERNAME` / `SMTP_PASSWORD`                                     | Helm deploy — mail config                |
| `SLACK_WEBHOOK`                                                       | Slack notifications                      |
| `GITHUB_API_ACCESS_TOKEN`                                             | GitHub release creation via `release-it` |
| `SVC_BUILD_GIT_USERNAME` / `SVC_BUILD_GIT_EMAIL`                      | Git commits during release               |
