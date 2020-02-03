package cn.easyact.fin.manager

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, YearMonth}
import java.util
import java.util.UUID
import java.util.UUID.randomUUID

case class MonthlyForecast(month: YearMonth, balance: Amount)

object MonthlyForecast {
  def apply(month: Int, balance: Amount)(implicit time: TimeService): MonthlyForecast =
    MonthlyForecast(YearMonth.of(time.today.getYear, month), balance)
}

case class BudgetUnit(no: String, name: String, start: LocalDate, balance: Amount = 0,
                      budgetItems: List[BudgetItem] = List.empty) extends Aggregate

sealed trait BudgetItem {
  val id: UUID
  val name: String
  val amount: Amount
  val intervalMonth: Int
  val start: LocalDate

  def getType: String
}

sealed trait Income extends BudgetItem {
  def getType = "Income"
}

sealed trait Expense extends BudgetItem {
  def getType = "Expense"
}

trait Factory[T <: BudgetItem] {
  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")

  def apply(map: util.Map[String, String]): T =
    apply(map.get("id"), map.get("项目"), map.get("数额").toDouble, map.get("月周期").toInt, map.get("开始月份"))

  def apply(id: String, t: String, amount: Amount, intervalMonth: Int, startMonth: String): T = {
    val start = YearMonth.parse(startMonth, formatter)
    apply(id, t, amount, intervalMonth, start)
  }

  def apply(id: String, t: String, amount: Amount, intervalMonth: Int, start: YearMonth): T =
    apply(id, t, amount, intervalMonth, start.atEndOfMonth())

  def apply(id: String, t: String, amount: Amount, intervalMonth: Int, start: LocalDate): T =
    apply(Option(id).map(UUID.fromString).getOrElse(randomUUID), t, amount, intervalMonth, start)

  def apply(id: UUID, t: String, amount: Amount, intervalMonth: Int, start: LocalDate): T
}

object Income extends Factory[Income] {
  override def apply(id: UUID, t: String, amount: Amount, intervalMonth: Int, start: LocalDate): Income = {
    t match {
      case "工资" => Salary(id, amount, intervalMonth, start)
      case _ => OtherIncome(id, amount, intervalMonth, start, t)
    }
  }
}

object Expense extends Factory[Expense] {
  def apply(id: UUID, t: String, amount: Amount, intervalMonth: Int, start: LocalDate): Expense = {
    t match {
      case _ => OtherExpense(id, amount, intervalMonth, start, t)
    }
  }
}

case class Salary(id: UUID, amount: Amount, intervalMonth: Int, start: LocalDate) extends Income {
  override val name: String = "工资"
}

case class OtherIncome(id: UUID, amount: Amount, intervalMonth: Int, start: LocalDate, name: String) extends Income

case class OtherExpense(id: UUID, amount: Amount, intervalMonth: Int, start: LocalDate, name: String) extends Expense