resource "aws_cognito_user_pool_client" "clients" {
  for_each = local.callbacks
  name = each.key
  user_pool_id = aws_cognito_user_pool.freedom.id
  allowed_oauth_flows = [
    "code", "implicit"]
  callback_urls = [
    each.value]
  supported_identity_providers = [
    "COGNITO"]
  allowed_oauth_scopes = [
    "openid"]
  allowed_oauth_flows_user_pool_client = true
}

resource "aws_cognito_user_pool" "freedom" {
  name = "freedom"
}
resource "aws_cognito_user_pool_domain" "freedom" {
  domain = "easyact"
  user_pool_id = aws_cognito_user_pool.freedom.id
}

locals {
  domain = "https://${aws_cognito_user_pool_domain.freedom.domain}.auth.ap-southeast-1.amazoncognito.com"
  callbacks = {
    prod = "https://freedom.easyact.cn"
    dev = "http://localhost:4200"
  }
}

output "cognito" {
  value = {
    url = {for k, v in local.callbacks : k=>{
      code = "${local.domain}/login?response_type=code&scope=openid&client_id=${aws_cognito_user_pool_client.clients[k].id}&redirect_uri=${aws_cognito_user_pool_client.clients[k].callback_urls[0]}"
      implicit = "${local.domain}/login?response_type=token&scope=openid&client_id=${aws_cognito_user_pool_client.clients[k].id}&redirect_uri=${aws_cognito_user_pool_client.clients[k].callback_urls[0]}"
      client_id = aws_cognito_user_pool_client.clients[k]
    }}
  }
}