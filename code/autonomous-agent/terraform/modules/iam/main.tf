# Execution role assumed by the AgentCore Runtime service principal.
resource "aws_iam_role" "this" {
  name = "${var.app_name}AgentCoreRuntimeRole"

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

# Least-privilege execution policy: ECR pull, logs, X-Ray, metrics, Bedrock invoke,
# and the specific AgentCore Memory resource only.
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
        Sid      = "ECRTokenAccess"
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Sid      = "Logs"
        Effect   = "Allow"
        Action   = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents", "logs:DescribeLogStreams"]
        Resource = ["arn:aws:logs:${var.region}:${var.account_id}:log-group:/aws/bedrock-agentcore/runtimes/*"]
      },
      {
        # logs:DescribeLogGroups does not support resource-level scoping to a
        # single group, so it is scoped to this account and region only and
        # further constrained to the AgentCore runtime log-group prefix.
        Sid      = "LogsDescribe"
        Effect   = "Allow"
        Action   = ["logs:DescribeLogGroups"]
        Resource = ["arn:aws:logs:${var.region}:${var.account_id}:log-group:*"]
        Condition = {
          StringEquals = { "aws:RequestedRegion" = var.region }
        }
      },
      {
        # X-Ray Put*/Get* trace APIs do not support resource-level permissions
        # and require Resource = "*". Constrain to this account and region.
        Sid      = "XRay"
        Effect   = "Allow"
        Action   = ["xray:PutTraceSegments", "xray:PutTelemetryRecords", "xray:GetSamplingRules", "xray:GetSamplingTargets"]
        Resource = ["*"]
        Condition = {
          StringEquals = { "aws:RequestedRegion" = var.region }
        }
      },
      {
        # cloudwatch:PutMetricData does not support resource-level permissions.
        # Constrain to the bedrock-agentcore namespace and this region.
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
        # Inference profiles are scoped to this account and region. Foundation
        # models keep a region wildcard because cross-region (global) inference
        # profiles route model invocations to foundation models in other
        # regions; the foundation-model ARN never carries an account id.
        Sid    = "BedrockModelInvocation"
        Effect = "Allow"
        Action = ["bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"]
        Resource = [
          "arn:aws:bedrock:*::foundation-model/*",
          "arn:aws:bedrock:${var.region}:${var.account_id}:inference-profile/*"
        ]
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
        Resource = [var.memory_resource_arn]
      }
    ]
  })
}
