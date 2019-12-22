package manager

import java.time.Instant

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task

trait Event[A] {
  def at: Instant

  def no: String
}

trait Aggregate {
  def no: AggregateId
}

trait Snapshot[A <: Aggregate] {
  def updateState(initial: Map[String, A], e: Event[_]): Map[String, A]

  def snapshot(es: List[Event[_]]): String \/ Map[String, A] =
    es.foldLeft(Map.empty[String, A])(updateState).right
}

trait Interpreter[A] {

  def step: Event ~> Task

  def apply(action: Free[Event, A]): Task[A] = action.foldMap(step)
}