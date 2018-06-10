package com.david.user

import com.david.item.UUIDable

object Domain {

  final case class Organisation(id: Option[String], name: String, email: String,
    typeOrganisation: String, address: Address) extends UUIDable

  object UserSalutation extends Enumeration {
    val MR, MS, MRS = Value
  }

  final case class User(id: Option[String], firstname: String, lastname: String,
    salutation: UserSalutation.Value, telephone: String,
    typeUser: String, address: Address, organizationId: Option[String]) extends UUIDable

  case class Address(street: String, number: String, postalCode: String, city: String, country: String)

  final case class UsersWithOrganisation(users: Seq[UserWithOrganisation])

  case class UserWithOrganisation(id: Option[String], firstname: String, lastname: String,
      salutation: UserSalutation.Value, telephone: String,
      typeUser: String, address: Address, organization: Option[Organisation]) {
    def getUser(): User = organization match {
      case None => User(id, firstname, lastname, salutation, telephone, typeUser, address, None)
      case Some(org) => User(id, firstname, lastname, salutation, telephone, typeUser, address, org.id)
    }
  }
}

