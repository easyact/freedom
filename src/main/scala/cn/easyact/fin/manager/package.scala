package cn.easyact.fin

import java.time.ZoneId

import com.typesafe.scalalogging.Logger
import scalaz.Scalaz._
import scalaz._

import scala.language.implicitConversions

package object manager {
  type AggregateId = String
  type Error = String

  type Amount = BigDecimal
  type Command[A] = Free[Event, A]

  val zone: ZoneId = ZoneId.systemDefault()

  Logger[ZoneId].info(s"当前时区: $zone")

  class Tappable[A](a: A) {
    def tap(action: A => Unit): A = {
      action(a)
      a
    }
  }

  implicit def any2Tappable[A](a: A): Tappable[A] = new Tappable[A](a)

  implicit def option2Either[A](o: Option[A]): String \/ A = o match {
    case Some(a) => a.right
    //    case None => throw new RuntimeException("Not found")
    case None => "Not found".left
  }
}
