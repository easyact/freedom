package manager

import scalaz.Scalaz._
import scalaz._

import scala.collection.concurrent.TrieMap

trait EventStore[K] {
  def get(key: K): List[Event[_]]

  def put(key: K, event: Event[_]): Error \/ Event[_]

  def events(key: K): Error \/ List[Event[_]]

  def allEvents: Error \/ List[Event[_]]

  def clear(): Unit
}

object MemEventStore {
  def apply[K]: EventStore[K] = new EventStore[K] {
    val evnetLog: TrieMap[K, List[Event[_]]] = TrieMap[K, List[Event[_]]]()

    override def clear(): Unit = evnetLog.clear

    override def get(key: K): List[Event[_]] = evnetLog.getOrElse(key, List.empty)

    override def put(key: K, event: Event[_]): Error \/ Event[_] = {
      val list: List[Event[_]] = evnetLog.getOrElse(key, Nil)
      evnetLog += (key -> (list :+ event))
      \/-(event)
    }

    override def events(key: K): Error \/ List[Event[_]] = {
      val option = evnetLog.get(key)
      if (option.isEmpty) s"Aggregate $key does not exist".left
      else option.get.right
    }

    override def allEvents: Error \/ List[Event[_]] = evnetLog.values.toList.flatten.right
  }
}