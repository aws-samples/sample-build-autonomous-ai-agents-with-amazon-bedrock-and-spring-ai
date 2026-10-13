output "cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "configure_kubectl" {
  description = "Command to configure kubectl for this cluster."
  value       = "aws eks update-kubeconfig --name ${module.eks.cluster_name} --region ${var.region}"
}

output "irsa_role_arn" {
  description = "Least-privilege IAM role bound to the app service account."
  value       = module.eks_irsa.role_arn
}

output "image_uri" {
  description = "Deployed container image."
  value       = local.image_uri
}

output "namespace" {
  description = "Namespace the agent runs in."
  value       = module.eks_workload.namespace
}

output "service_dns" {
  description = "In-cluster service DNS name."
  value       = module.eks_workload.service_dns
}

output "load_balancer_hostname" {
  description = "External endpoint (populates shortly after apply)."
  value       = module.eks_workload.load_balancer_hostname
}
