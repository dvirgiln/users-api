package com.david.user

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import Domain._


//#user-case-classes

object UserActor {
  final case object GetUsers
  final case class CreateUser(user: UserWithOrganisation)
  final case class UpdateUser(user: UserWithOrganisation)
  final case class GetUser(id: String)
  final case class DeleteUser(id: String)
  def props: Props = Props[UserActor]
}


class UserActor extends Actor with ActorLogging {
  import UserActor._
  import FullUserService._


  def receive: Receive = {
    case GetUsers =>
      sender() ! getUsers
    case CreateUser(user) =>
      sender() ! create(user)
    case UpdateUser(user) =>
      val updated = update(user)
      sender() ! updated
    case GetUser(id) =>
      sender() ! getUser(id)
    case DeleteUser(id) =>
      sender() ! deleteUser(id)
  }
}
//#user-registry-actor