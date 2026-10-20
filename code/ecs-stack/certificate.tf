# TLS certificate for the HTTPS listeners.
#
# Preferred: pass certificate_arn for an ACM certificate that covers
# domain_name, and point a DNS record for domain_name at the load balancer.
#
# Default (no domain): generate a self-signed certificate for the load balancer
# DNS name and import it into ACM. Traffic is encrypted, but browsers warn once
# per port until you accept the certificate. The private key is kept in
# Terraform state, which is git-ignored; use certificate_arn for anything
# beyond a short validation.

resource "tls_private_key" "self_signed" {
  count = var.certificate_arn == "" ? 1 : 0

  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "tls_self_signed_cert" "self_signed" {
  count = var.certificate_arn == "" ? 1 : 0

  private_key_pem       = tls_private_key.self_signed[0].private_key_pem
  validity_period_hours = 24 * 90
  early_renewal_hours   = 24 * 7

  subject {
    common_name  = local.host
    organization = "Spring AI AgentCore sample"
  }
  dns_names = [local.host]

  allowed_uses = ["key_encipherment", "digital_signature", "server_auth"]
}

resource "aws_acm_certificate" "self_signed" {
  count = var.certificate_arn == "" ? 1 : 0

  private_key      = tls_private_key.self_signed[0].private_key_pem
  certificate_body = tls_self_signed_cert.self_signed[0].cert_pem

  lifecycle {
    create_before_destroy = true
  }
}

locals {
  certificate_arn = var.certificate_arn != "" ? var.certificate_arn : aws_acm_certificate.self_signed[0].arn
}
