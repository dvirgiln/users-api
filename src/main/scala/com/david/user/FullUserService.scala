package com.david.user

import com.david.item.StoreService
import com.david.user.Domain.Organisation
import Domain._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/*
 * OrganisationService store class. Makes use of the generic store service.
 */
object OrganisationService extends StoreService[Organisation] {
  override def storeId(id: String, item: Organisation): Organisation = item.copy(Some(id))
}

/*
 * UserService store class. Makes use of the generic store service.
 */
object UserService extends StoreService[User] {
  override def storeId(id: String, item: User): User = item.copy(Some(id))
}

/*
 * This class contains the logic for all the CRUD operations related User with Organizations
 * It calls the different storage services and join the Future calls.
 */
object FullUserService {
  def getUser(id: String): Future[Option[UserWithOrganisation]] = {
    UserService.get(id).flatMap { user =>
      user match {
        case Some(user) => user.organizationId match {
          case Some(organisationId) => OrganisationService.get(organisationId).map(org => Some(joinUserAndOrganisation(user, org)))
          case None => Future(Some(joinUserAndOrganisation(user, None)))
        }
        case None => Future(None)
      }
    }
  }

  def getUsers(): Future[Seq[UserWithOrganisation]] = {
    val a = UserService.keys.flatMap(keys => Future.sequence(keys.map(id => getUser(id))))
    a.map(result => result.flatten)
  }

  def deleteUser(id: String): Future[Boolean] = UserService.remove(id)

  def create(newUser: UserWithOrganisation): Future[UserWithOrganisation] = {
    for {
      user <- UserService.add(newUser.getUser())
      organisationOpt <- saveOrganisation(newUser.organization)

    } yield {
      //Important, once the organisation is created, it is required to update the user with the organisationId
      organisationOpt match {
        case Some(organisation) => UserService.update(user.copy(organizationId = organisation.id))
        case None =>
      }
      joinUserAndOrganisation(user, organisationOpt)
    }
  }

  def update(updatedUser: UserWithOrganisation): Future[Boolean] = {
    saveOrganisation(updatedUser.organization)
    UserService.update(updatedUser.getUser())
  }

  private def saveOrganisation(organisation: Option[Organisation]): Future[Option[Organisation]] = {
    organisation match {
      case Some(org @ Organisation(Some(id), _, _, _, _)) => OrganisationService.update(org).map { updateResult =>
        updateResult match {
          case true => Some(org)
          case false => None
        }
      }
      case Some(org @ Organisation(None, _, _, _, _)) => OrganisationService.add(org).map(Some(_))
      case None => Future(None)
    }
  }

  /*
 * Utility function used to merge an user with an Organisation.
 */
  def joinUserAndOrganisation(user: User, organisation: Option[Organisation]): UserWithOrganisation = {
    UserWithOrganisation(user.id, user.firstname, user.lastname, user.salutation,
      user.telephone, user.typeUser, user.address, organisation)
  }
}
