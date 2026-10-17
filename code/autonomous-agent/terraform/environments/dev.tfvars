region      = "us-east-1"
environment = "dev"
app_name    = "autonomous-agent"
# image_tag is not set here: scripts/deploy.sh passes the tag it just pushed.
memory_id = ""

tags = {
  CostCenter = "ml-platform"
  Owner      = "agent-team"
}
