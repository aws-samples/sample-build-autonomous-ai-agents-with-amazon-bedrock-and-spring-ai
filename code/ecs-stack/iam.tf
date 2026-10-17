# --- Task execution role: pull images and write logs (all services) -----------

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [local.account_id]
    }
    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = ["arn:aws:ecs:${var.region}:${local.account_id}:*"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${local.prefix}-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# --- Backend task role: Bedrock + AgentCore Browser / Code Interpreter --------
# Same permissions the AgentCore Runtime execution role grants the agents, minus
# ECR/logs (handled by the execution role). No memory: the backends run with the
# "stateless" profile in this stack.

data "aws_iam_policy_document" "backend" {
  statement {
    sid     = "BedrockInferenceProfile"
    actions = ["bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"]
    resources = [
      "arn:aws:bedrock:${var.region}:${local.account_id}:inference-profile/*"
    ]
  }

  # Global cross-Region inference also authorizes the Region-less foundation
  # model ARN; scope it to calls made through this account's inference profiles.
  statement {
    sid       = "BedrockFoundationModelViaProfile"
    actions   = ["bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"]
    resources = ["arn:aws:bedrock:*::foundation-model/*"]
    condition {
      test     = "StringLike"
      variable = "bedrock:InferenceProfileArn"
      values   = ["arn:aws:bedrock:${var.region}:${local.account_id}:inference-profile/*"]
    }
  }

  statement {
    sid = "AgentCoreBrowser"
    actions = [
      "bedrock-agentcore:StartBrowserSession",
      "bedrock-agentcore:GetBrowserSession",
      "bedrock-agentcore:StopBrowserSession",
      "bedrock-agentcore:UpdateBrowserStream",
      "bedrock-agentcore:ConnectBrowserAutomationStream"
    ]
    resources = [
      "arn:aws:bedrock-agentcore:${var.region}:aws:browser/*",
      "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:browser/*"
    ]
  }

  statement {
    sid = "AgentCoreCodeInterpreter"
    actions = [
      "bedrock-agentcore:StartCodeInterpreterSession",
      "bedrock-agentcore:InvokeCodeInterpreter",
      "bedrock-agentcore:GetCodeInterpreterSession",
      "bedrock-agentcore:StopCodeInterpreterSession"
    ]
    resources = [
      "arn:aws:bedrock-agentcore:${var.region}:aws:code-interpreter/*",
      "arn:aws:bedrock-agentcore:${var.region}:${local.account_id}:code-interpreter/*"
    ]
  }
}

resource "aws_iam_role" "backend" {
  name               = "${local.prefix}-backend"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_iam_role_policy" "backend" {
  name   = "agent-backend"
  role   = aws_iam_role.backend.id
  policy = data.aws_iam_policy_document.backend.json
}
