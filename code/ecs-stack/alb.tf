# One load balancer, one HTTPS listener per web app:
#   :443  landing page   (:80 redirects here)
#   :8081 travel agent UI
#   :8082 IDP console
# TLS terminates at the load balancer with a TLS 1.2+ policy. Pass
# certificate_arn (and domain_name) for a trusted certificate; otherwise the
# stack imports a self-signed certificate into ACM (see certificate.tf).

resource "aws_lb" "this" {
  #checkov:skip=CKV_AWS_91:Access logs need a separate S3 bucket (SSE-S3 only); add one for long-lived use
  #checkov:skip=CKV2_AWS_28:Ingress is limited to allowed_cidrs (0.0.0.0/0 is rejected); attach AWS WAF if exposed more widely
  name               = local.prefix
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  # Streaming chat and document extraction can take a while.
  idle_timeout               = 180
  drop_invalid_header_fields = true

  # scripts/cleanup.sh turns this off before terraform destroy.
  enable_deletion_protection = var.alb_deletion_protection
}

resource "aws_lb_target_group" "web" {
  #checkov:skip=CKV_AWS_378:TLS terminates at the load balancer; targets are in private subnets reachable only from the ALB security group
  for_each = local.web_apps

  name        = substr("${local.prefix}-${each.value.short}", 0, 32)
  port        = local.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.this.id

  deregistration_delay = 15

  health_check {
    path                = "/healthz"
    matcher             = "200"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "web" {
  for_each = local.web_apps

  load_balancer_arn = aws_lb.this.arn
  port              = local.listener_ports[each.key]
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = local.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.web[each.key].arn
  }
}

# Plain HTTP on :80 only redirects to the HTTPS landing page.
resource "aws_lb_listener" "http_redirect" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}
