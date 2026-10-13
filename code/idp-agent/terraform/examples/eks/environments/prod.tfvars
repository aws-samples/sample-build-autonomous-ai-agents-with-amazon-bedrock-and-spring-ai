region              = "us-east-1"
environment         = "prod"
app_name            = "idp-agent"
kubernetes_version  = "1.33"
node_instance_types = ["m7g.large"]
node_desired_size   = 3
node_min_size       = 3
node_max_size       = 6
ecr_repository_name = "idp-agent"
image_tag           = "latest"
# Set to a real memory id created with scripts/create-memory.sh
memory_id            = ""
replicas             = 3
cors_allowed_origins = "https://idp.example.com"

# Restrict to your operator/CI egress ranges. Never 0.0.0.0/0.
cluster_endpoint_public_access_cidrs = ["203.0.113.0/24"]
load_balancer_internal               = true

tags = {
  CostCenter = "ml-platform"
  Owner      = "idp-team"
  Compliance = "pii"
}
