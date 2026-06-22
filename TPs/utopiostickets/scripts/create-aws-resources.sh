#!/usr/bin/env bash
set -euo pipefail

REGION="${AWS_REGION:-eu-west-3}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo "=== Creating AWS resources for UtopiosTickets ==="
echo "Region     : $REGION"
echo "Account ID : $ACCOUNT_ID"
echo ""

# ── DynamoDB ──────────────────────────────────────────────────────────────────

echo "Creating DynamoDB table: utopios-events"
aws dynamodb create-table \
  --table-name utopios-events \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" \
  --output text --query 'TableDescription.TableName'

echo "Creating DynamoDB table: utopios-bookings (with GSI customerName-index)"
aws dynamodb create-table \
  --table-name utopios-bookings \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=customerName,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --global-secondary-indexes '[
    {
      "IndexName": "customerName-index",
      "KeySchema": [{"AttributeName":"customerName","KeyType":"HASH"}],
      "Projection": {"ProjectionType":"ALL"}
    }
  ]' \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" \
  --output text --query 'TableDescription.TableName'

echo "Waiting for tables to be ACTIVE..."
aws dynamodb wait table-exists --table-name utopios-events  --region "$REGION"
aws dynamodb wait table-exists --table-name utopios-bookings --region "$REGION"
echo "DynamoDB tables ready."

# ── S3 ────────────────────────────────────────────────────────────────────────

TICKETS_BUCKET="utopios-tickets-${ACCOUNT_ID}"
REPORTS_BUCKET="utopios-reports-${ACCOUNT_ID}"

create_bucket() {
  local BUCKET="$1"
  echo "Creating S3 bucket: $BUCKET"
  aws s3api create-bucket \
    --bucket "$BUCKET" \
    --region "$REGION" \
    --create-bucket-configuration LocationConstraint="$REGION"

  aws s3api put-public-access-block \
    --bucket "$BUCKET" \
    --public-access-block-configuration \
      "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

  aws s3api put-bucket-encryption \
    --bucket "$BUCKET" \
    --server-side-encryption-configuration \
      '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"},"BucketKeyEnabled":true}]}'

  echo "  Public-access block : enabled"
  echo "  Encryption           : SSE-S3 (AES256)"
}

create_bucket "$TICKETS_BUCKET"
create_bucket "$REPORTS_BUCKET"

# ── Summary ───────────────────────────────────────────────────────────────────

echo ""
echo "=== Resources created successfully ==="
echo "  DynamoDB : utopios-events"
echo "  DynamoDB : utopios-bookings  (GSI: customerName-index)"
echo "  S3       : $TICKETS_BUCKET"
echo "  S3       : $REPORTS_BUCKET"
echo ""
echo "Next step — update k8s/configmap.yaml with:"
echo "  S3_TICKETS_BUCKET: \"$TICKETS_BUCKET\""
echo "  S3_REPORTS_BUCKET: \"$REPORTS_BUCKET\""
echo ""
echo "Then create the Kubernetes secret:"
echo "  kubectl create secret generic aws-credentials \\"
echo "    --namespace utopios \\"
echo "    --from-literal=AWS_ACCESS_KEY_ID=\$AWS_ACCESS_KEY_ID \\"
echo "    --from-literal=AWS_SECRET_ACCESS_KEY=\$AWS_SECRET_ACCESS_KEY"
