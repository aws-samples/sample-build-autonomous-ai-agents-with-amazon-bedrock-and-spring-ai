# Remote state backend (S3 + DynamoDB lock).
#
# State is kept local by default so the project works out of the box. For team
# use, store state remotely in a HARDENED S3 bucket with a DynamoDB table for
# state locking. The bucket and table names are account and team specific, so
# this block stays commented. Uncomment it and supply your own values, or pass
# them at init time with a partial backend config.
#
# Terraform state can contain sensitive values (ARNs, tokens, generated
# secrets), so the state bucket must be encrypted, versioned, private, and
# reachable only over TLS.
#
# One-time bootstrap (run once per account, outside this configuration):
#
#   BUCKET=my-tf-state-bucket
#   REGION=us-east-1
#   KMS_KEY_ARN=arn:aws:kms:us-east-1:111122223333:key/xxxxxxxx  # customer-managed key
#
#   1. Create the bucket:
#        aws s3api create-bucket --bucket "$BUCKET" --region "$REGION"
#
#   2. Enable versioning (recover from accidental or malicious state changes):
#        aws s3api put-bucket-versioning --bucket "$BUCKET" \
#          --versioning-configuration Status=Enabled
#
#   3. Enforce SSE-KMS with a customer-managed key and an S3 Bucket Key
#      (lowers KMS request cost):
#        aws s3api put-bucket-encryption --bucket "$BUCKET" \
#          --server-side-encryption-configuration '{
#            "Rules":[{
#              "ApplyServerSideEncryptionByDefault":{
#                "SSEAlgorithm":"aws:kms","KMSMasterKeyID":"'"$KMS_KEY_ARN"'"},
#              "BucketKeyEnabled":true
#            }]
#          }'
#
#   4. Block all public access:
#        aws s3api put-public-access-block --bucket "$BUCKET" \
#          --public-access-block-configuration \
#          BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
#
#   5. Deny all non-TLS (HTTP) access with a bucket policy:
#        aws s3api put-bucket-policy --bucket "$BUCKET" --policy '{
#          "Version":"2012-10-17",
#          "Statement":[{
#            "Sid":"DenyInsecureTransport",
#            "Effect":"Deny",
#            "Principal":"*",
#            "Action":"s3:*",
#            "Resource":["arn:aws:s3:::'"$BUCKET"'","arn:aws:s3:::'"$BUCKET"'/*"],
#            "Condition":{"Bool":{"aws:SecureTransport":"false"}}
#          }]
#        }'
#
#   6. Create the DynamoDB lock table with a "LockID" (string) partition key
#      (encryption at rest is enabled by default with an AWS-owned key; supply
#      --sse-specification for a customer-managed key):
#        aws dynamodb create-table --table-name my-tf-locks \
#          --attribute-definitions AttributeName=LockID,AttributeType=S \
#          --key-schema AttributeName=LockID,KeyType=HASH \
#          --billing-mode PAY_PER_REQUEST
#
# Then either uncomment the block below, or keep it commented and run:
#   terraform init \
#     -backend-config="bucket=my-tf-state-bucket" \
#     -backend-config="key=idp-agent/dev/terraform.tfstate" \
#     -backend-config="region=us-east-1" \
#     -backend-config="dynamodb_table=my-tf-locks" \
#     -backend-config="kms_key_id=$KMS_KEY_ARN" \
#     -backend-config="encrypt=true"
#
# Use a distinct "key" per environment (for example idp-agent/dev/... and
# idp-agent/prod/...) so environments never share state.
#
# terraform {
#   backend "s3" {
#     bucket         = "my-tf-state-bucket"
#     key            = "idp-agent/dev/terraform.tfstate"
#     region         = "us-east-1"
#     dynamodb_table = "my-tf-locks"
#     encrypt        = true
#     kms_key_id     = "arn:aws:kms:us-east-1:111122223333:key/xxxxxxxx"
#   }
# }
