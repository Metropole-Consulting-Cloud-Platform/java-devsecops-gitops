# Java DevSecOps: CI, CD, and GitOps

A runnable Java 21 / Spring Boot service and delivery pipeline. The GitHub
Actions path builds and tests the service, scans source and the container
image, publishes an immutable image to GitHub Container Registry (GHCR), and
updates the Kubernetes image tag in Git. Argo CD watches that Git path and
applies the change to a Kubernetes cluster.

The repository also includes a GitLab CI pipeline for teams using GitLab.
Choose one CI provider for a given branch/release process rather than running
both pipelines as competing publishers.

## What CI, CD, GitHub Actions, GitLab CI, and GitOps mean

- **Git** is version control: it records the source and configuration changes.
- **CI (continuous integration)** automatically builds, tests, and checks
  changes when they are proposed or pushed. Both GitHub Actions and GitLab CI
  are CI/CD automation platforms.
- **CD** can mean continuous delivery (a deployable, verified change is
  prepared for approval) or continuous deployment (verified changes deploy
  automatically). In this repository, image publication happens after checks,
  and merging the new image tag into the watched Git branch lets Argo CD
  deploy it automatically.
- **GitOps** is a way to operate deployment environments: Git holds the
  desired Kubernetes configuration, and an in-cluster controller such as
  Argo CD continually reconciles the cluster to that configuration. GitOps is
  an operating model, not another CI product.

In this setup: **GitHub Actions or GitLab CI = CI and image build/publish;
Argo CD = continuous deployment/GitOps; Git = source and desired state.**
Argo CD pulls desired state from Git; the CI runner does not need cluster
administrator credentials.

## Repository layout

```text
java-devsecops-gitops/
├── .github/workflows/java-devsecops.yml   # GitHub CI, image publishing, GitOps tag update
├── .gitlab-ci.yml                         # GitLab CI alternative
├── app/                                   # Spring Boot service and tests
│   ├── pom.xml
│   └── src/
├── docker/
│   └── Dockerfile                         # Non-root Java 21 container
├── docs/
│   └── pipeline-comparison.md
└── k8s/
    ├── base/                              # Deployment, Service, Kustomize config
    └── argocd/
        └── application.yaml               # Argo CD watches this repository
```

## Run and test locally

Requirements: JDK 21, Maven 3.9+, and Docker for the container steps.

From the repository root in PowerShell or a shell:

```powershell
cd app
mvn test
mvn spring-boot:run
```

In another terminal, call the real service endpoints:

```powershell
curl.exe http://localhost:8080/api/v1/hello
curl.exe http://localhost:8080/actuator/health
```

Expected greeting response:

```json
{"message":"Hello from Java DevSecOps","service":"java-devsecops-api"}
```

Build and run the same application in Docker:

```powershell
docker build -f docker/Dockerfile -t java-devsecops-api:local .
docker run --rm -p 8080:8080 java-devsecops-api:local
```

## GitHub Actions pipeline

`.github/workflows/java-devsecops.yml` runs on pull requests and pushes to
`main`:

1. Maven compiles the Java 21 application and runs JUnit tests.
2. Semgrep performs Java static analysis; Trivy scans the repository for
   vulnerable dependencies, secrets, and unsafe configuration.
3. After successful checks on `main`, the workflow builds the container,
   scans the image for HIGH/CRITICAL vulnerabilities, and pushes it to
   `ghcr.io/asimshahibm/java-devsecops-gitops` with a commit-specific tag.
4. The workflow opens or updates a pull request that sets the immutable image
   tag in `k8s/base/kustomization.yaml`. Review and merge that GitOps change;
   Argo CD then detects the change on `main` and syncs the Kubernetes
   resources.

The image publication uses the repository's built-in `GITHUB_TOKEN`; it does
not require a personal access token or cloud access key. In the GitHub
repository settings, ensure Actions can write packages and contents. After
the first image publication, set the GHCR package visibility to **public** if
the cluster should pull without credentials; otherwise create an
`imagePullSecret` in the `java-devsecops` namespace using a read-only package
credential. Never commit that credential.

## Connect Argo CD to a real Kubernetes cluster

The committed Argo CD `Application` uses the actual repository URL
`https://github.com/asimshahibm/java-devsecops-gitops.git`, watches `main`,
and syncs `k8s/base` into the `java-devsecops` namespace. Install Argo CD into
your cluster first, then apply the application manifest:

```sh
kubectl apply -f k8s/argocd/application.yaml
```

If your Argo CD installation cannot read the repository, grant it read-only
access to this private repository using an Argo CD repository credential
Secret or deploy key. Do not put the credential in this repository.

For a local smoke test without Argo CD, install `kubectl` and Kustomize and
apply the same manifests to a Kubernetes cluster you control:

```sh
kubectl apply -k k8s/base
kubectl rollout status deployment/java-devsecops-api -n java-devsecops
kubectl port-forward -n java-devsecops service/java-devsecops-api 8080:80
```

Then request `http://localhost:8080/api/v1/hello`. A newly published private
GHCR image requires an image pull secret; see the GHCR instructions above.

## GitLab CI alternative

`.gitlab-ci.yml` runs the same Maven tests, Semgrep and Trivy scans, builds
and scans an image in GitLab's built-in Container Registry, then pushes an
image-tag update to the `ci/gitops-image` branch. Enable the GitLab Container
Registry and allow `CI_JOB_TOKEN` to push to the repository under the
project's CI/CD job-token permissions. Review and merge that branch to the
default branch to trigger Argo CD (open a merge request from the branch if
your project does not merge branches directly). The GitLab runner must permit
Docker-in-Docker (privileged runner). Configure Argo CD to watch the GitLab
repository and grant it read-only repository access. Configure image pull
credentials in the cluster when that registry is private. The included
GitLab file is an alternative pipeline; it does not run when only pushed to
GitHub.

## Security and production notes

- The sample API is intentionally stateless and has no database or passwords.
  Add real infrastructure endpoints and credentials only when the app needs
  them; keep credentials in a secret manager or protected CI/cluster secret.
- Container image tags are updated to a commit-specific tag rather than a
  mutable `latest` tag.
- Kubernetes runs as a non-root user, drops Linux capabilities, applies CPU
  and memory requests/limits, and uses health probes and a disruption budget.
- Review scan findings and update pinned dependencies/base images regularly.
  The CI workflow fails when Trivy finds HIGH or CRITICAL findings.
- The repository does not contain your cluster address or cloud credentials:
  no cloud provider/account/cluster was specified. Argo CD is the deployment
  agent, so the GitHub Actions workflow does not need a kubeconfig or cluster
  admin secret.
