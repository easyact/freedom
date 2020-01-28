package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util.UUID.randomUUID
import java.util.{Map => JMap}

import cn.easyact.fin.manager.BudgetUnitCommands._
import cn.easyact.fin.manager.MemInterpreter
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.{LogManager, Logger}

class BuHandler extends RequestHandler[JMap[String, Object], BuResponse] {

  val logger: Logger = LogManager.getLogger(getClass)

  val mapper = new ObjectMapper()

  def handleRequest(in: JMap[String, Object], context: Context): BuResponse = {
    logger.info(s"Received a request: $in")
    val name = in.get("name").asInstanceOf[String]
    val id = randomUUID.toString
    val script = register(id, name, Some(LocalDate now()))
    MemInterpreter(script)
    val body = mapper.writeValueAsString(BU(id, name))
    BuResponse(200, body)
  }
}
