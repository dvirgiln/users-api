package com.david.user

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MessageEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import com.david.user.Domain.{Address, Organisation, UserSalutation, UserWithOrganisation}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._

class UserRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with UserRoutes with BeforeAndAfterAll {

  implicit lazy val timeout = Timeout(5.seconds)

  val userActor: ActorRef = system.actorOf(UserActor.props, "userRegistryActor")

  val address = Address("Street name", "189A", "WK134", "London", "UK")
  lazy val routes = userRoutes

  override def beforeAll() {
    UserService.init
  }

  "UserRoutes" should {
    "return the initial empty list of users (GET /users)" in {
      val request = HttpRequest(uri = "/users")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"users":[]}""")
      }
    }

    "add one user with an empty organization (POST /users)" in {
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, None)
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        val returnValue = entityAs[UserWithOrganisation]
        assert(checkEqualsUsers(returnValue, user, false) == true)
        assert(returnValue.id != None)
        returnValue.organization should ===( user.organization)
      }
    }

    "add one user with a new organization (POST /users)" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        val returnValue = entityAs[UserWithOrganisation]
        val equalsUsers= checkEqualsUsers(returnValue, user, false)
        equalsUsers shouldBe true
        assert(returnValue.id != None)
        assert(returnValue.organization != None)
        assert(checkEqualOrganisation(organisation, returnValue.organization.get, false) == true)
        assert(returnValue.organization.get.id != None)
      }
    }

    "add one user with a non existing organization returns a saved user with an empy organization(POST /users)" in {
      val organisation = Organisation(Some("123132132"), "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        val returnValue = entityAs[UserWithOrganisation]
        assert(checkEqualsUsers(returnValue, user, false) == true)
        assert(returnValue.id != None)
        returnValue.organization shouldBe None
      }
    }


    "add one user with and update an existing organization (POST /users)" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        val returnValue = entityAs[UserWithOrganisation]
        val equalsUsers= checkEqualsUsers(returnValue, user, false)
        equalsUsers shouldBe true
        assert(returnValue.id != None)
        assert(checkEqualOrganisation(organisation, returnValue.organization.get, false) == true)
        assert(returnValue.organization.get.id != None)

        //SECOND CALL, UPDATE OF ORGANISATION CREATING A SECOND USER
        val updatedOrganisation= returnValue.organization.get.copy(name= "UpdatedOrg")
        val user2= user.copy(firstname = "Marius", organization = Some(updatedOrganisation))
        val userEntity2 = Marshal(user2).to[MessageEntity].futureValue
        val request2 = Post(uri = "/users").withEntity(userEntity2)
        request2 ~> routes ~> check {
          status should ===(StatusCodes.Created)
          contentType should ===(ContentTypes.`application/json`)
          val returnValue2 = entityAs[UserWithOrganisation]
          assert(checkEqualsUsers(returnValue2, user2, false) == true)
          assert(returnValue.id != None)
          //Asserting that the organisations are the same, including the id, thats why the 3th parameter is true
          assert(checkEqualOrganisation(updatedOrganisation, returnValue2.organization.get, true) == true)
        }
      }
    }

    "return a 400 when the user to be created contains already an ID (POST /users)" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(Some("1221"), "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("""The ID should not be included for new resources""")
      }
    }

    "delete a user" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        val returnValue = entityAs[UserWithOrganisation]
        val request2 = Delete(uri = s"/users/${returnValue.id.get}")

        request2 ~> routes ~> check {
          status should ===(StatusCodes.OK)
        }
      }
    }

    "return an error when it is tried to delete a non existing user" in {
        Delete(uri = s"/users/fdsdfs") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

    "get a user (GET /users)" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        val returnValue = entityAs[UserWithOrganisation]
        val request2 = HttpRequest(uri = s"/users/${returnValue.id.get}")

        request2 ~> routes ~> check {
          status should ===(StatusCodes.OK)
          contentType should ===(ContentTypes.`application/json`)
          entityAs[UserWithOrganisation] should ===(returnValue)
        }
      }


    }



    "update user details in case user exist already (PUT /users)" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Post(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        val returnValue = entityAs[UserWithOrganisation]
        val updatedUser = returnValue.copy(telephone = "+44568965668")
        val userEntity2 = Marshal(updatedUser).to[MessageEntity].futureValue
        val request2 = Put(uri = "/users").withEntity(userEntity2)

        request2 ~> routes ~> check {
          status should ===(StatusCodes.OK)
        }
      }
    }

    "return an error 400 in case the id is not provided as part of the update (PUT /users)" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(None, "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Put(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("""The ID should be included for updating new resources""")
      }
    }

    "return an error 404 in case the id of the user provided doesnt exist (PUT /users)" in {
      val organisation = Organisation(None, "MyOrganization", "myorganization@gmail.com", "+44565896558", address)
      val user = UserWithOrganisation(Some("fdsadfsa"), "David", "Virgil", UserSalutation.MR, "+447956801230", "genericUser", address, Some(organisation))
      val userEntity = Marshal(user).to[MessageEntity].futureValue
      val request = Put(uri = "/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }
  }

  private def checkEqualsUsers(a: UserWithOrganisation, b: UserWithOrganisation, checkId: Boolean): Boolean = {
    a.firstname == b.firstname &&
    a.lastname == b.lastname &&
    a.salutation == b.salutation &&
    a.address == b.address &&
    a.telephone == b.telephone &&
    a.typeUser == b.typeUser && (!checkId || (a.id == b.id))
  }

  private def checkEqualOrganisation(a: Organisation, b: Organisation, checkId: Boolean): Boolean = {
    a.name == b.name &&
      a.email == b.email &&
      a.typeOrganisation == b.typeOrganisation &&
      a.address == b.address && (!checkId || (a.id == b.id))
  }

}
