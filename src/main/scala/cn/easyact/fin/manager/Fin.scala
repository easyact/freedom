package cn.easyact.fin.manager

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, YearMonth}
import java.util
import java.util.UUID
import java.util.UUID.randomUUID

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode, JsonSerializer, SerializerProvider}
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

case class MonthlyForecast(month: YearMonth, balance: Amount)

object MonthlyForecast {
  def apply(month: Int, balance: Amount)(implicit time: TimeService): MonthlyForecast =
    MonthlyForecast(YearMonth.of(time.today.getYear, month), balance)
}

case class BudgetUnit(no: String, name: String, start: LocalDate, balance: Amount = 0,
                      budgetItems: List[BudgetItem] = List.empty) extends Aggregate

@JsonDeserialize(using = classOf[BudgetItemDeserializer])
@JsonSerialize(using = classOf[BudgetItemSerializer])
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

  def apply(id: String, t: String, amount: Amount, intervalMonth: Int, start: LocalDate): T = {
    val uuid = Option(id).map(_.trim).filterNot(_.isBlank).map(UUID.fromString).getOrElse(randomUUID)
    apply(uuid, t, amount, intervalMonth, start)
  }

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

class BudgetItemDeserializer extends JsonDeserializer[BudgetItem] {
  private val log: Logger = Logger[BudgetItemDeserializer]

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): BudgetItem = {
    val node: JsonNode = p.getCodec.readTree(p)
    val textNode = node.get("dtype")
    log.debug(s"Desering $textNode: $node")
    val clz = Class.forName(textNode.toString)
    p.readValueAs(clz).asInstanceOf
  }
}

class BudgetItemSerializer extends JsonSerializer[BudgetItem] {
  override def serialize(value: BudgetItem, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeStringField("dtype", value.getType)
    gen.writeObject(value)
  }
}