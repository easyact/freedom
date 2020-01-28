package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util.UUID.randomUUID
import java.util.{Map => JMap}

import cn.easyact.fin.manager.BudgetUnitCommands._
import cn.easyact.fin.manager.MemInterpreter
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.{LogManager, Logger}

class BuHandler extends RequestHandler[APIGatewayProxyRequestEvent, BuResponse] {

  val logger: Logger = LogManager.getLogger(getClass)

  val mapper = new ObjectMapper()

  def handleRequest(in: APIGatewayProxyRequestEvent, context: Context): BuResponse = {
    logger.info(s"Received a request: $in")
    val sBu = in.getBody
    val bu = mapper.readValue(sBu, classOf[BU])
    val name = bu.name
    val id = randomUUID.toString
    val script = register(id, name, Some(LocalDate now()))
    MemInterpreter(script)
    val body = mapper.writeValueAsString(BU(id, name))
    BuResponse(200, body)
  }
}
