output "namespace" {
  description = "Namespace the workload runs in."
  value       = kubernetes_namespace_v1.this.metadata[0].name
}

output "service_account" {
  description = "Service account name bound to the IRSA role."
  value       = kubernetes_service_account_v1.this.metadata[0].name
}

output "service_name" {
  description = "In-cluster Kubernetes service name."
  value       = kubernetes_service_v1.this.metadata[0].name
}

output "service_dns" {
  description = "In-cluster service DNS name."
  value       = "${kubernetes_service_v1.this.metadata[0].name}.${kubernetes_namespace_v1.this.metadata[0].name}.svc.cluster.local"
}

output "load_balancer_hostname" {
  description = "External load balancer hostname when service_type is LoadBalancer (may take a minute to populate)."
  value       = try(kubernetes_service_v1.this.status[0].load_balancer[0].ingress[0].hostname, "")
}
