output "runtime_name" {
  description = "AgentCore Runtime name"
  value       = aws_bedrockagentcore_agent_runtime.this.agent_runtime_name
}

output "runtime_arn" {
  description = "AgentCore Runtime ARN"
  value       = aws_bedrockagentcore_agent_runtime.this.agent_runtime_arn
}

output "runtime_id" {
  description = "AgentCore Runtime id (use with get-agent-runtime)"
  value       = aws_bedrockagentcore_agent_runtime.this.agent_runtime_id
}
