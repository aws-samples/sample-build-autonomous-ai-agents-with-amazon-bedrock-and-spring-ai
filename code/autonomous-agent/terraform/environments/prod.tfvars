region      = "us-east-1"
environment = "prod"
app_name    = "autonomous-agent"
image_tag   = "latest"
# Set to a real memory id created with scripts/create-memory.sh
memory_id = ""

tags = {
  CostCenter = "ml-platform"
  Owner      = "agent-team"
}
