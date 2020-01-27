package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util.UUID.randomUUID
import java.util.{Map => JMap}

import cn.easyact.fin.manager.BudgetUnitCommands._
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.logging.log4j.{LogManager, Logger}

class BuHandler extends RequestHandler[JMap[String, String], BU] {

  val logger: Logger = LogManager.getLogger(getClass)

  def handleRequest(in: JMap[String, String], context: Context): BU = {
    logger.info(s"Received a request: $in")
    val name = in get "name"
    val id = randomUUID.toString
    register(id, name, Some(LocalDate now()))
    new BU(id, name)
  }
}
