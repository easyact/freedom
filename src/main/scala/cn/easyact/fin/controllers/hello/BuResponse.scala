package cn.easyact.fin.controllers.hello

import scala.beans.BeanProperty
import java.util.{Map => JMap}

case class BuResponse(@BeanProperty statusCode: Integer, @BeanProperty body: String,
                      @BeanProperty headers: JMap[String, String] = JMap.of(), @BeanProperty base64Encoded: Boolean = false)
