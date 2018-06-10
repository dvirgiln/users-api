package com.david.user

import com.david.user.Domain.{Address, _}
import spray.json.{DeserializationException, JsString, RootJsonFormat, _}

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
    override def write(obj: T#Value): JsValue = JsString(obj.toString)

    override def read(json: JsValue): T#Value = {
      json match {
        case JsString(txt) => enu.withName(txt)
        case somethingElse => throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
      }
    }
  }
  implicit val enumConverter = new EnumJsonConverter(UserSalutation)
  implicit val addressJsonFormat = jsonFormat5(Address)
  implicit val organisationJsonFormat = jsonFormat5(Organisation)
  implicit val userWithOrganisationJsonFormat = jsonFormat8(UserWithOrganisation)
  implicit val usersWithOrganisationJsonFormat = jsonFormat1(UsersWithOrganisation)

}
