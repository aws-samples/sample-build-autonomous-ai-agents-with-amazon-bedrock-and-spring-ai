output "landing_url" {
  description = "Landing page linking both agents."
  value       = local.urls.landing
}

output "travel_ui_url" {
  description = "Travel agent UI."
  value       = local.urls.travel_ui
}

output "idp_ui_url" {
  description = "IDP console."
  value       = local.urls.idp_ui
}

output "ecr_repository_urls" {
  description = "ECR repositories for the landing page and UI images (push here before the second apply)."
  value       = { for k, r in aws_ecr_repository.web : k => r.repository_url }
}

output "cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.this.name
}

output "log_groups" {
  description = "CloudWatch log groups per service."
  value       = { for k, g in aws_cloudwatch_log_group.service : k => g.name }
}
