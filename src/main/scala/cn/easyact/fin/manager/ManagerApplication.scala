package cn.easyact.fin.manager

import java.util.UUID

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

class ManagerApplication extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {
  override def handleRequest(req: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val bu = req.getBody()
//    new ObjectMapper()
//    BudgetUnitCommands.register(UUID.randomUUID(), bu.)
    val responseEvent = new APIGatewayProxyResponseEvent
    responseEvent.setBody(req.getBody)
    responseEvent
  }

}