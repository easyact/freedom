package manager

import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

class ManagerApplication extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {
  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val responseEvent = new APIGatewayProxyResponseEvent
    responseEvent.setBody(dispatch(input))
    responseEvent
  }

  def dispatch(input: APIGatewayProxyRequestEvent) = (input.getHttpMethod, input.getPath) match {
    case ("POST", "/budget-units") =>
      input.getBody
    case _ => input.getBody
  }
}