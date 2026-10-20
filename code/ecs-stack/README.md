# ECS web stack: browser URLs for both agents

AgentCore Runtime only forwards `/invocations` and `/ping`, so the React UIs and the
landing page cannot run there. This stack runs them on Amazon ECS Fargate (ARM64) next to
the same backend images the AgentCore deploys pushed:

```
you (allowed IP) --HTTPS--> Application Load Balancer (public subnets)
                              :443  landing-page          (:80 redirects to :443)
                              :8081 autonomous-agent-ui --Service Connect--> autonomous-agent
                              :8082 idp-agent-ui        --Service Connect--> idp-agent
                            ECS tasks in private subnets --NAT gateway--> ECR, Bedrock, AgentCore
```

- The backends are not public. Each UI's nginx proxies `/api` and `/ping` to its backend.
- Backends run with `SPRING_PROFILES_ACTIVE=<env>,stateless` (no AgentCore Memory) and a
  task role for Amazon Bedrock plus AgentCore Browser and Code Interpreter.
- The load balancer accepts only `allowed_cidrs`; `0.0.0.0/0` is rejected because the UIs
  have no real authentication (the IDP sign-in is a demo).
- Security defaults: tasks have no public IPs; TLS 1.2+ on every listener; a customer-managed
  KMS key (rotation on) encrypts the ECR repositories and log groups; logs are kept 365 days;
  VPC flow logs, Container Insights, and load balancer deletion protection are on.
- Cost: roughly $90-120/month while running (load balancer, NAT gateway, five small Fargate
  tasks, KMS key). Destroy it when you are done.

### Certificate

| Setting | Result |
|---|---|
| `certificate_arn` and `domain_name` set | Trusted ACM certificate. Point a DNS record for `domain_name` at the load balancer. |
| Both empty (default) | A self-signed certificate for the load balancer DNS name is imported into ACM. Browsers warn once per port (443, 8081, 8082); accept each before the landing page status checks turn green. Its private key is in Terraform state (git-ignored). |

Use a real certificate for anything beyond a short validation.

## Deploy

Prerequisites: both AgentCore stacks are deployed (their `terraform/image-version.txt`
files name the backend images), plus Terraform >= 1.6, AWS CLI, and Docker with buildx.

```bash
cd code/ecs-stack
./scripts/deploy.sh dev                         # allows your current public IP
ALLOWED_CIDR=203.0.113.10/32 ./scripts/deploy.sh dev
```

The script prints the three URLs. Open the landing page; both cards should show
**UI: running** and **Backend: running**.

### Without Docker (Jib CLI)

Each web folder has a `jib.yaml` build file equivalent to its Dockerfile. With
[Jib CLI](https://github.com/GoogleContainerTools/jib/tree/master/jib-cli) installed:

```bash
terraform apply -var-file=environments/dev.tfvars              # phase 1
export VITE_LANDING_URL="$(terraform output -raw landing_url)/"
TAG=$(date +%Y%m%d%H%M%S); REG=<account>.dkr.ecr.<region>.amazonaws.com
for app in landing-page autonomous-agent-ui idp-agent-ui; do
  (cd ../$app && npm ci && npm run build && \
   jib build --build-file=jib.yaml --parameter=CREATION_TIME=$(date -u +%FT%TZ) \
     --target=$REG/agentcore-web-dev-$app:$TAG \
     --to-username=AWS --to-password="$(aws ecr get-login-password)")
done
terraform apply -var-file=environments/dev.tfvars -var ui_image_tag=$TAG   # services
```

## Validate

| Check | Expected |
|---|---|
| `<landing_url>` | Landing page, both cards **running** |
| `<travel_ui_url>/ping` | `{"status":"Healthy",...}` |
| `<idp_ui_url>/api/agents` | `["LAWYER","MORTGAGE","INSURANCE","TAX","GENERAL"]` |
| Travel UI: "Plan a 3-day trip to Tokyo" | Streamed answer using the weather and flight tools |
| IDP console: upload `idp-agent/samples/lease-agreement.pdf` as Lawyer | Unsigned and high-risk **ALERT**s, item in the Review queue |

Logs: CloudWatch log groups `/ecs/agentcore-web-<env>/<service>`.

## Clean up

```bash
./scripts/cleanup.sh dev
```

The script first turns off load balancer deletion protection, then destroys the services,
load balancer, NAT gateway, VPC, and the three web ECR repositories, and schedules the KMS
key for deletion (7 days). The
AgentCore Runtime stacks and their repositories are separate; clean them up with their own
`scripts/cleanup.sh`.
