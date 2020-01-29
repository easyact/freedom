package cn.easyact.fin.controllers.hello

import scala.beans.BeanProperty

case class BU(@BeanProperty no: String, @BeanProperty name: String) {
  override def toString: String = s"BU($no, $name)"
}

