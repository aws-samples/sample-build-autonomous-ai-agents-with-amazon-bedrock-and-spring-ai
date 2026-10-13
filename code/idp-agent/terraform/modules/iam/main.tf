# Execution role assumed by the AgentCore Runtime service principal.
resource "aws_iam_role" "this" {
  name = "${var.app_name}AgentCoreRuntimeRole"

  # Confused-deputy guards: only the AgentCore service, acting on behalf of THIS
  # account (aws:SourceAccount) and only for AgentCore resources in this account
  # and region (aws:SourceArn), may assume the role.
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AssumeRolePolicy"
        Effect    = "Allow"
        Principal = { Service = "bedrock-agentcore.amazonaws.com" }
        Action    = "sts:AssumeRole"
        Condition = {
          StringEquals = { "aws:SourceAccount" = var.account_id }
          ArnLike      = { "aws:SourceArn" = "arn:aws:bedrock-agentcore:${var.region}:${var.account_id}:*" }
        }
      }
    ]
  })

  tags = var.tags
}

# Least-privilege execution policy.
#
# Every statement is scoped to this account and region. Where an action does not
# support resource-level permissions (ecr:GetAuthorizationToken, the xray:* and
# cloudwatch:PutMetricData telemetry actions), the "*" resource is constrained by
# an aws:RequestedRegion condition and, for metrics, a namespace condition. These
# residual wildcards are documented in SECURITY.md.
resource "aws_iam_role_policy" "this" {
  name = "AgentCoreExecutionPolicy"
  role = aws_iam_role.this.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "ECRImageAccess"
        Effect   = "Allow"
        Action   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
        Resource = [var.ecr_repository_arn]
      },
      {
        # ecr:GetAuthorizationToken does not support resource-level permissions;
        # constrain it to the deployment region instead.
        Sid       = "ECRTokenAccess"
        Effect    = "Allow"
        Action    = ["ecr:GetAuthorizationToken"]
        Resource  = "*"
        Condition = { StringEquals = { "aws:RequestedRegion" = var.region } }
      },
      {
        Sid       = "Logs"
        Effect    = "Allow"
        Action    = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents", "logs:DescribeLogStreams"]
        Resource  = ["arn:aws:logs:${var.region}:${var.account_id}:log-group:/aws/bedrock-agentcore/runtimes/*"]
        Condition = { StringEquals = { "aws:RequestedRegion" = var.region } }
      },
      {
        Sid       = "LogsDescribe"
        Effect    = "Allow"
        Action    = ["logs:DescribeLogGroups"]
        Resource  = ["arn:aws:logs:${var.region}:${var.account_id}:log-group:*"]
        Condition = { StringEquals = { "aws:RequestedRegion" = var.region } }
      },
      {
        # X-Ray write actions do not support resource-level permissions; constrain
        # to the deployment region.
        Sid       = "XRay"
        Effect    = "Allow"
        Action    = ["xray:PutTraceSegments", "xray:PutTelemetryRecords", "xray:GetSamplingRules", "xray:GetSamplingTargets"]
        Resource  = ["*"]
        Condition = { StringEquals = { "aws:RequestedRegion" = var.region } }
      },
      {
        # cloudwatch:PutMetricData does not support resource-level permissions;
        # constrain to the deployment region and the agent metric namespace.
        Sid      = "CloudWatchMetrics"
        Effect   = "Allow"
        Action   = "cloudwatch:PutMetricData"
        Resource = "*"
        Condition = {
          StringEquals = {
            "cloudwatch:namespace" = "bedrock-agentcore"
            "aws:RequestedRegion"  = var.region
          }
        }
      },
      {
        Sid    = "BedrockModelInvocation"
        Effect = "Allow"
        Action = ["bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"]
        Resource = [
          "arn:aws:bedrock:${var.region}::foundation-model/*",
          "arn:aws:bedrock:${var.region}:${var.account_id}:inference-profile/*"
        ]
        Condition = { StringEquals = { "aws:RequestedRegion" = var.region } }
      },
      {
        Sid    = "AgentCoreMemoryAccess"
        Effect = "Allow"
        Action = [
          "bedrock-agentcore:CreateEvent",
          "bedrock-agentcore:ListEvents",
          "bedrock-agentcore:DeleteEvent",
          "bedrock-agentcore:RetrieveMemoryRecords"
        ]
        Resource  = [var.memory_resource_arn]
        Condition = { StringEquals = { "aws:RequestedRegion" = var.region } }
      }
    ]
  })
}
