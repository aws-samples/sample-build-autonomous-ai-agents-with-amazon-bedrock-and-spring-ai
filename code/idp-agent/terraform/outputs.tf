output "ecr_repository_url" {
  description = "ECR repository to push the agent image to"
  value       = module.ecr.repository_url
}

output "runtime_name" {
  description = "AgentCore Runtime name used when invoking the agent"
  value       = module.runtime.runtime_name
}

output "runtime_arn" {
  description = "AgentCore Runtime ARN"
  value       = module.runtime.runtime_arn
}

output "runtime_id" {
  description = "AgentCore Runtime id (use with aws bedrock-agentcore-control get-agent-runtime)"
  value       = module.runtime.runtime_id
}

output "runtime_role_arn" {
  description = "IAM execution role assumed by the runtime"
  value       = module.iam.role_arn
}
