region      = "us-east-1"
environment = "dev"
app_name    = "idp-agent"
# image_tag is not set here: scripts/deploy.sh passes the tag it just pushed.
memory_id = ""

tags = {
  CostCenter = "ml-platform"
  Owner      = "idp-team"
}
