#!/usr/bin/env bash
# AWS WAF v2 — .env 스캔 봇 차단 룰 설정
# 실행 전: AWS CLI 로그인 + 아래 변수 확인
# 실행: bash aws/setup-waf.sh

set -euo pipefail

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
CLUSTER_NAME="burntz-cluster"
SERVICE_NAME="burntz-service"
WAF_NAME="burntz-waf"

echo "▶ [1/5] ECS 서비스에 연결된 ALB ARN 조회"
ALB_ARN=$(aws ecs describe-services \
  --cluster "$CLUSTER_NAME" \
  --services "$SERVICE_NAME" \
  --region "$AWS_REGION" \
  --query "services[0].loadBalancers[0].targetGroupArn" \
  --output text 2>/dev/null || echo "")

if [ -z "$ALB_ARN" ] || [ "$ALB_ARN" = "None" ]; then
  echo "  ⚠️  ECS 서비스에서 ALB를 찾지 못했습니다. 직접 ALB ARN을 조회합니다."
  aws elbv2 describe-load-balancers \
    --region "$AWS_REGION" \
    --query "LoadBalancers[*].{Name:LoadBalancerName, ARN:LoadBalancerArn, DNS:DNSName}" \
    --output table
  echo ""
  read -rp "  위 목록에서 사용할 ALB ARN을 입력하세요: " ALB_ARN
else
  # targetGroupArn으로 ALB ARN 역추적
  ALB_ARN=$(aws elbv2 describe-target-groups \
    --target-group-arns "$ALB_ARN" \
    --region "$AWS_REGION" \
    --query "TargetGroups[0].LoadBalancerArns[0]" \
    --output text)
  echo "  ✅ ALB ARN: $ALB_ARN"
fi

echo ""
echo "▶ [2/5] WAF Web ACL 생성 (이미 있으면 스킵)"
EXISTING_ACL=$(aws wafv2 list-web-acls \
  --scope REGIONAL \
  --region "$AWS_REGION" \
  --query "WebACLs[?Name=='$WAF_NAME'].ARN" \
  --output text 2>/dev/null || echo "")

if [ -n "$EXISTING_ACL" ] && [ "$EXISTING_ACL" != "None" ]; then
  WEB_ACL_ARN="$EXISTING_ACL"
  echo "  ✅ 기존 Web ACL 사용: $WEB_ACL_ARN"
else
  WEB_ACL_ARN=$(aws wafv2 create-web-acl \
    --name "$WAF_NAME" \
    --scope REGIONAL \
    --region "$AWS_REGION" \
    --default-action Allow={} \
    --visibility-config \
      SampledRequestsEnabled=true,CloudWatchMetricsEnabled=true,MetricName=burntzWaf \
    --rules '[
      {
        "Name": "BlockDotEnvScan",
        "Priority": 1,
        "Action": { "Block": {} },
        "Statement": {
          "ByteMatchStatement": {
            "SearchString": ".env",
            "FieldToMatch": { "UriPath": {} },
            "TextTransformations": [
              { "Priority": 0, "Type": "LOWERCASE" }
            ],
            "PositionalConstraint": "ENDS_WITH"
          }
        },
        "VisibilityConfig": {
          "SampledRequestsEnabled": true,
          "CloudWatchMetricsEnabled": true,
          "MetricName": "BlockDotEnvScan"
        }
      },
      {
        "Name": "AWSManagedRulesCommonRuleSet",
        "Priority": 10,
        "OverrideAction": { "None": {} },
        "Statement": {
          "ManagedRuleGroupStatement": {
            "VendorName": "AWS",
            "Name": "AWSManagedRulesCommonRuleSet"
          }
        },
        "VisibilityConfig": {
          "SampledRequestsEnabled": true,
          "CloudWatchMetricsEnabled": true,
          "MetricName": "AWSManagedRulesCommonRuleSet"
        }
      }
    ]' \
    --query "Summary.ARN" \
    --output text)
  echo "  ✅ Web ACL 생성 완료: $WEB_ACL_ARN"
fi

echo ""
echo "▶ [3/5] WAF를 ALB에 연결"
ASSOC_RESULT=$(aws wafv2 get-web-acl-for-resource \
  --resource-arn "$ALB_ARN" \
  --region "$AWS_REGION" \
  --query "WebACL.Name" \
  --output text 2>/dev/null || echo "")

if [ "$ASSOC_RESULT" = "$WAF_NAME" ]; then
  echo "  ✅ 이미 연결되어 있습니다."
else
  aws wafv2 associate-web-acl \
    --web-acl-arn "$WEB_ACL_ARN" \
    --resource-arn "$ALB_ARN" \
    --region "$AWS_REGION"
  echo "  ✅ ALB에 WAF 연결 완료"
fi

echo ""
echo "▶ [4/5] 설정 확인"
aws wafv2 get-web-acl \
  --name "$WAF_NAME" \
  --scope REGIONAL \
  --region "$AWS_REGION" \
  --id "$(echo "$WEB_ACL_ARN" | awk -F/ '{print $NF}')" \
  --query "{Name: WebACL.Name, Rules: WebACL.Rules[*].{Name:Name, Priority:Priority}}" \
  --output table 2>/dev/null || echo "  (get-web-acl 확인은 콘솔에서 직접 확인하세요)"

echo ""
echo "▶ [5/5] CloudWatch 메트릭 확인 방법"
echo "  콘솔 → WAF & Shield → burntz-waf → Sampled requests 탭"
echo "  또는:"
echo "  aws cloudwatch get-metric-statistics \\"
echo "    --namespace AWS/WAFV2 \\"
echo "    --metric-name BlockedRequests \\"
echo "    --dimensions Name=WebACL,Value=$WAF_NAME Name=Rule,Value=BlockDotEnvScan Name=Region,Value=$AWS_REGION \\"
echo "    --start-time \$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ) \\"
echo "    --end-time \$(date -u +%Y-%m-%dT%H:%M:%SZ) \\"
echo "    --period 3600 --statistics Sum --region $AWS_REGION"

echo ""
echo "🎉 완료. .env 스캔 봇이 ALB 레벨에서 403으로 차단됩니다."
echo "   Spring 앱 로그에도 더 이상 이 요청들이 찍히지 않습니다."
