package cn.easyact.fin.manager

import java.util.UUID

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.typesafe.scalalogging.Logger
import BudgetUnitCommands._

class ManagerApplication extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {
  val log: Logger = Logger[ManagerApplication]

  override def handleRequest(req: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val bu = req.getBody
//        new ObjectMapper()
    //    BudgetUnitCommands.register(UUID.randomUUID(), bu.)
    log debug s"收到请求: $bu"
//    register(UUID.randomUUID().toString, name, Some(today), now)
    val responseEvent = new APIGatewayProxyResponseEvent
    responseEvent.setBody(req.getBody)
    responseEvent
  }

}