package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util.UUID.randomUUID

import cn.easyact.fin.manager.BudgetUnitCommands._
import cn.easyact.fin.manager.{BudgetUnit, BudgetUnitSnapshot, MemInterpreter}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.{LogManager, Logger}
import scalaz.Scalaz._
import scalaz._

class BuHandler extends RequestHandler[APIGatewayProxyRequestEvent, BuResponse] {

  val logger: Logger = LogManager.getLogger(getClass)

  val mapper = new ObjectMapper

  import BudgetUnitSnapshot._
  import cn.easyact.fin.manager.MemInterpreter.eventLog._
  import mapper._

  def handleRequest(in: APIGatewayProxyRequestEvent, context: Context): BuResponse = {
    logger.info(s"Received a request: $in")

    def post = {
      val sBu = in.getBody
      val dto = readValue(sBu, classOf[BU])
      val name = dto.name
      val id = randomUUID.toString
      val script = register(id, name, Some(LocalDate now()))
      val task = MemInterpreter(script)
      val bu = task.unsafePerformSync
      bu
    }

    def get = allEvents.flatMap(snapshot).fold(
      err => throw new RuntimeException(err),
      m => m
    )

    val body = in.getHttpMethod match {
      case "GET" => get
      case "POST" => post
    }
    BuResponse(200, writeValueAsString(body))
  }
}
