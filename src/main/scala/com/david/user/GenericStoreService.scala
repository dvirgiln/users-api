package com.david.item

import java.util.UUID

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
trait UUIDable {
  val id: Option[String]
}

trait StoreService[T <: UUIDable] {
  private var items = mutable.Map.empty[String, T]

  def init() = {
    items = mutable.Map.empty[String, T]
  }

  def getAll(): Future[Seq[T]] = Future(items.values.toSeq.sortBy(_.id))

  def add(item: T): Future[T] = {
    Future {
      val uuid = UUID.randomUUID.toString
      val itemToStore = storeId(uuid, item)
      items += (uuid -> itemToStore)
      itemToStore
    }
  }

  def update(item: T): Future[Boolean] = {
    Future(
      items.get(item.id.get).headOption match {
        case Some(_) =>
          items(item.id.get) = item
          true
        case None => false
      }
    )
  }

  def remove(id: String): Future[Boolean] = {
    Future(
      if (exists(id)) {
        items -= (id)
        true
      }
      else {
        false
      }
    )
  }

  def get(id: String): Future[Option[T]] = Future(items.get(id))

  def keys : Future[Seq[String]] = Future(items.keys.toSeq)

  private def exists(id: String): Boolean = items.contains(id)

  def storeId(id: String, item: T): T

  init()
}
