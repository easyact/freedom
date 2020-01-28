package cn.easyact.fin.controllers.hello

import scala.beans.BeanProperty

case class BU(@BeanProperty var id: String, @BeanProperty var name: String) {
  override def toString: String = s"BU($id, $name)"
}

