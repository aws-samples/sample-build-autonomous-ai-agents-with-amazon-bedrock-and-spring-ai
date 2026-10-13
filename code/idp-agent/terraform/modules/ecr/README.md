# ecr

## Purpose

Creates an Amazon ECR repository for the agent container image with immutable tags
by default, scan-on-push, encryption at rest (ECR-managed AES256 by default, or a
customer-managed KMS key when `kms_key_arn` is set), an optional resource-based
repository policy, and a lifecycle policy that expires untagged images and keeps
only the most recent tagged images.

## Requirements

| Name | Version |
|------|---------|
| terraform | >= 1.6 |
| aws | ~> 6.0 |

## Providers

| Name | Version |
|------|---------|
| aws | ~> 6.0 |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| repository_name | Name of the ECR repository. | `string` | n/a | yes |
| image_tag_mutability | MUTABLE or IMMUTABLE. | `string` | `"IMMUTABLE"` | no |
| kms_key_arn | Optional customer-managed KMS key ARN. Empty means ECR-managed AES256. | `string` | `""` | no |
| repository_policy_json | Optional JSON resource-based repository policy. Empty means none. | `string` | `""` | no |
| force_delete | Allow deleting the repository even if it contains images. | `bool` | `true` | no |
| keep_last_images | Number of tagged images to retain. | `number` | `10` | no |
| tags | Tags applied to the repository. | `map(string)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| repository_url | Repository URL for docker push. |
| repository_arn | Repository ARN. |
| repository_name | Repository name. |

## Usage

```hcl
module "ecr" {
  source          = "./modules/ecr"
  repository_name = "idp-agent-dev"
  tags            = { Application = "idp-agent" }
}
```
