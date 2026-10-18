# Pipeline comparison

| Tool | Category | Role in this repository | Does it deploy? |
| --- | --- | --- | --- |
| Git | Version control | Stores source and Kubernetes desired state | No |
| GitHub Actions | CI/CD automation | Tests, scans, builds and publishes the image, then opens a GitOps pull request | Argo CD deploys after the reviewed image-tag change is merged |
| GitLab CI | CI/CD automation | Alternative Maven, security scan, and image build/publish pipeline; pushes the tag change to a Git branch | Argo CD deploys after the image-tag branch is merged |
| Argo CD | GitOps continuous delivery | Watches a Git branch/path and reconciles Kubernetes to its manifests | Yes, automatically when automated sync is enabled |
| Kubernetes | Container orchestrator | Runs and heals the desired application resources | Runs the app; it does not build or test source |

CI and CD describe activities; GitHub Actions and GitLab CI are products that
can automate them. GitOps describes the pull-based deployment model used here.
This avoids giving an external CI runner long-lived Kubernetes credentials.

## Event flow

```text
Developer push / pull request
          |
          v
GitHub Actions OR GitLab CI
  Maven tests -> code/security checks -> image build and scan -> registry
                                                          |
                     GitOps PR / MR with image tag <------+
                                  |
                           review and merge
                                  |
                                  v
                         Argo CD watches Git
                                  |
                                  v
                           Kubernetes cluster
```

For production, protect `main`, require successful checks and review, scope
registry permissions, and configure Argo CD repository/image access through
secrets outside Git.
