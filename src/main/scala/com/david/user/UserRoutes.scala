package com.david.user

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.david.user.UserActor.{CreateUser, DeleteUser, GetUser, GetUsers, UpdateUser}

import scala.concurrent.Future



trait UserRoutes extends JsonSupport {
  import Domain._
  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem
  lazy val logUser = Logging(system, classOf[UserRoutes])
  implicit val timeout: Timeout

  def userActor: ActorRef

  lazy val userRoutes: Route =
    pathPrefix("users") {
      concat(
        path(Segment) { userId =>
          concat(
            get {
              logUser.info(s"Getting user $userId")
              val maybeUser: Future[Option[UserWithOrganisation]] =
                (userActor ? GetUser(userId)).mapTo[Future[Option[UserWithOrganisation]]].flatten
              onSuccess(maybeUser) { response =>
                response match {
                  case Some(user) => complete(user)

                  case None => complete(StatusCodes.NotFound)
                }
              }
            },
            delete {
              logUser.info(s"Deleting user $userId")
              val responseFuture: Future[Boolean] = (userActor ? DeleteUser(userId)).mapTo[Future[Boolean]].flatten
              onSuccess(responseFuture) { response =>
                response match {
                  case true => complete(StatusCodes.OK)
                  case false => complete(StatusCodes.NotFound)
                }
              }
            }
          )
        },
        pathEnd {
          concat(
            get {
              val usersFuture: Future[Seq[UserWithOrganisation]] =
                (userActor ? GetUsers).mapTo[Future[Seq[UserWithOrganisation]]].flatten
              onSuccess(usersFuture) { users =>
                complete(UsersWithOrganisation(users))
              }
            },
            post {
              entity(as[UserWithOrganisation]) { user =>
                user.id match {
                  case None =>
                    val userCreated: Future[UserWithOrganisation] =
                      (userActor ? CreateUser(user)).mapTo[Future[UserWithOrganisation]].flatten
                    onSuccess(userCreated) { user =>
                      complete((StatusCodes.Created, user))
                    }
                  case Some(_) =>
                    complete(StatusCodes.BadRequest, "The ID should not be included for new resources")
                }

              }
            },
            put {
              entity(as[UserWithOrganisation]) { user =>
                user.id match {
                  case None =>
                    complete(StatusCodes.BadRequest, "The ID should be included for updating new resources")
                  case Some(_) =>
                    val userUpdated: Future[Boolean] =
                      (userActor ? UpdateUser(user)).mapTo[Future[Boolean]].flatten
                    onSuccess(userUpdated) { updated =>
                      updated match {
                        case true => complete(StatusCodes.OK)
                        case false => complete(StatusCodes.NotFound)
                      }

                    }
                }

              }
            }

          )
        }
      )
    }
}
