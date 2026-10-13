output "role_arn" {
  description = "Execution role ARN"
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "Execution role name"
  value       = aws_iam_role.this.name
}
