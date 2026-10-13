region              = "us-east-1"
environment         = "dev"
app_name            = "idp-agent"
kubernetes_version  = "1.33"
node_instance_types = ["t4g.large"]
node_desired_size   = 2
node_min_size       = 2
node_max_size       = 4
ecr_repository_name = "idp-agent"
image_tag           = "latest"
replicas            = 2
memory_id           = ""

# Restrict to your operator/CI egress ranges. Never 0.0.0.0/0.
cluster_endpoint_public_access_cidrs = ["203.0.113.0/24"]
load_balancer_internal               = true

tags = {
  CostCenter = "ml-platform"
  Owner      = "idp-team"
}
