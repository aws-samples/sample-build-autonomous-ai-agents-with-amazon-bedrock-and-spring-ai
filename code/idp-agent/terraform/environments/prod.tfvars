region      = "us-east-1"
environment = "prod"
app_name    = "idp-agent"
# image_tag is not set here: scripts/deploy.sh passes the tag it just pushed.
# Set to a real memory id created with scripts/create-memory.sh
memory_id = ""

tags = {
  CostCenter = "ml-platform"
  Owner      = "idp-team"
  Compliance = "pii"
}
