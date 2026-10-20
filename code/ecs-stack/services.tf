# Five Fargate services (ARM64 / Graviton):
#   backends  autonomous-agent, idp-agent   (Service Connect servers, not public)
#   web apps  landing, travel-ui, idp-ui    (behind the load balancer)
# The UIs reach their backend at http://autonomous-agent:8080 and
# http://idp-agent:8080 through Service Connect, the same names docker compose uses.

locals {
  # Browsers send an Origin header that nginx forwards to the backend, so each
  # backend must allow its UI's public origin (CORS_ALLOWED_ORIGINS). Without
  # this, Spring rejects the UI's /api calls with "403 Invalid CORS request".
  backends = {
    autonomous-agent = {
      image  = var.autonomous_agent_image
      cpu    = 1024
      memory = 2048
      env = {
        BEDROCK_MODEL        = var.bedrock_model
        CORS_ALLOWED_ORIGINS = local.urls.travel_ui
      }
    }
    idp-agent = {
      image  = var.idp_agent_image
      cpu    = 1024
      memory = 2048
      env = {
        BEDROCK_MODEL        = var.bedrock_model
        CORS_ALLOWED_ORIGINS = local.urls.idp_ui
      }
    }
  }

  web_services = {
    landing = {
      log     = "landing"
      backend = null
      env = {
        AUTONOMOUS_UI_URL = local.urls.travel_ui
        IDP_UI_URL        = local.urls.idp_ui
        # Backends are not public; each UI proxies /ping to its backend.
        AUTONOMOUS_API_URL = local.urls.travel_ui
        IDP_API_URL        = local.urls.idp_ui
      }
    }
    travel_ui = {
      log     = "travel-ui"
      backend = "autonomous-agent"
      env     = { BACKEND_URL = "http://autonomous-agent:8080" }
    }
    idp_ui = {
      log     = "idp-ui"
      backend = "idp-agent"
      env     = { BACKEND_URL = "http://idp-agent:8080" }
    }
  }

  runtime_platform = {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64"
  }
}

# --- Backends -----------------------------------------------------------------

resource "aws_ecs_task_definition" "backend" {
  for_each = local.backends

  family                   = "${local.prefix}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.backend.arn

  runtime_platform {
    operating_system_family = local.runtime_platform.operating_system_family
    cpu_architecture        = local.runtime_platform.cpu_architecture
  }

  container_definitions = jsonencode([{
    name      = each.key
    image     = each.value.image
    essential = true
    portMappings = [{
      name          = "http"
      containerPort = local.container_port
      protocol      = "tcp"
      appProtocol   = "http"
    }]
    environment = [for k, v in merge({
      AWS_REGION             = var.region
      LOG_FORMAT             = "ecs"
      SPRING_PROFILES_ACTIVE = "${var.environment},stateless"
    }, each.value.env) : { name = k, value = v }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.service[each.key].name
        awslogs-region        = var.region
        awslogs-stream-prefix = each.key
      }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  for_each = local.deploy_services ? local.backends : {}

  name            = each.key
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.backend[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.tasks.id]
    assign_public_ip = false
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.this.arn

    service {
      port_name      = "http"
      discovery_name = each.key
      client_alias {
        port     = local.container_port
        dns_name = each.key
      }

      # Service Connect (Envoy) cuts requests off after 15 seconds by default.
      # Document extraction and streamed chat replies can take minutes, so match
      # the load balancer's idle timeout and nginx's proxy_read_timeout.
      timeout {
        per_request_timeout_seconds = 300
        idle_timeout_seconds        = 300
      }
    }
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  # Tasks pull images and call AWS APIs through the NAT gateway.
  depends_on = [aws_route_table_association.private]
}

# --- Web apps (landing page and UIs) --------------------------------------------

resource "aws_ecs_task_definition" "web" {
  for_each = local.deploy_services ? local.web_services : {}

  family                   = "${local.prefix}-${each.value.log}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.execution.arn

  runtime_platform {
    operating_system_family = local.runtime_platform.operating_system_family
    cpu_architecture        = local.runtime_platform.cpu_architecture
  }

  container_definitions = jsonencode([{
    name      = each.value.log
    image     = "${aws_ecr_repository.web[each.key].repository_url}:${var.ui_image_tag}"
    essential = true
    portMappings = [{
      name          = "http"
      containerPort = local.container_port
      protocol      = "tcp"
      appProtocol   = "http"
    }]
    environment = [for k, v in each.value.env : { name = k, value = v }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.service[each.value.log].name
        awslogs-region        = var.region
        awslogs-stream-prefix = each.value.log
      }
    }
  }])
}

resource "aws_ecs_service" "web" {
  for_each = local.deploy_services ? local.web_services : {}

  name                              = each.value.log
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.web[each.key].arn
  desired_count                     = 1
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 30

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.web[each.key].arn
    container_name   = each.value.log
    container_port   = local.container_port
  }

  # UIs are Service Connect clients so they can resolve their backend by name.
  dynamic "service_connect_configuration" {
    for_each = each.value.backend == null ? [] : [1]
    content {
      enabled   = true
      namespace = aws_service_discovery_http_namespace.this.arn
    }
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener.web, aws_ecs_service.backend, aws_route_table_association.private]
}
