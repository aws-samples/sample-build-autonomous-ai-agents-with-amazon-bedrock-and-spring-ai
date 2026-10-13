output "role_arn" {
  description = "IRSA role ARN to annotate on the Kubernetes service account."
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "IRSA role name."
  value       = aws_iam_role.this.name
}
