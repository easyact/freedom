package cn.easyact.fin.controllers.hello

import java.util

import cn.easyact.fin.manager.{Expense, Income}

import scala.beans.BeanProperty

case class BU(@BeanProperty no: String, @BeanProperty name: String) {
  override def toString: String = s"BU($no, $name)"
}

case class Items(incomes: util.List[Income], expenses: util.List[Expense])