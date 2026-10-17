# One load balancer, one listener per web app:
#   :80   landing page
#   :8081 travel agent UI
#   :8082 IDP console
# HTTP only: there is no domain or certificate in this demo stack. Add an ACM
# certificate and HTTPS listeners before using it beyond a quick validation.

resource "aws_lb" "this" {
  name               = local.prefix
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  # Streaming chat and document extraction can take a while.
  idle_timeout               = 180
  drop_invalid_header_fields = true
}

resource "aws_lb_target_group" "web" {
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
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.web[each.key].arn
  }
}
