package com.ntoggle.kubitschek.domain

import com.ntoggle.albi.DemandPartnerId
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.playjson.FormatAsString
import com.ntoggle.goldengate.scalazint.{OrderzByString, EqualzAndShowz}
import play.api.libs.json._

import scalaz._

case class UserListName(value: String) extends AnyVal
object UserListName extends FormatAsString[UserListName]
with OrderzByString[UserListName]
{
  val valueLens: UserListName @> String =
    Lens.lensu((rn, v) => rn.copy(value = v), _.value)
}

case class UserList(
  id: UserListId,
  name: UserListName,
  dpId: DemandPartnerId,
  created: CreatedInstant,
  modified: ModifiedInstant)

object UserList
  extends EqualzAndShowz[Version] {
  implicit val formatVersion: Format[UserList] =
    Json.format[UserList]
}

