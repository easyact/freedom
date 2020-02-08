provider "aws" {
  region = "ap-southeast-1"
//  endpoints {
//    dynamodb = "http://localhost:8000"
//  }
}
resource "aws_dynamodb_table" "events" {
  hash_key = "no"
  range_key = "at"
  name = "freedomEvents"

  attribute {
    name = "no"
    type = "S"
  }
  attribute {
    name = "at"
    type = "S"
  }
  read_capacity = 1
  write_capacity = 1
}
