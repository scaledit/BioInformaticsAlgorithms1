package com.ntoggle.kubitschek.services

import com.ntoggle.albi.DemandPartnerId
import com.ntoggle.albi.users.UserListId
import com.ntoggle.kubitschek.domain._
import scala.concurrent.{ExecutionContext, Future}

object UserListService {

  def get(
    getUserList: UserListId => Future[Option[UserList]])(implicit ctx: ExecutionContext):
  UserListId => Future[Option[UserList]] = { id =>
      getUserList(id).map(_.map(
        result => UserList(
          result.id,
          result.name,
          result.dpId,
          result.created,
          result.modified
        )))
  }

  def getByName(
    getUserList: (UserListName, DemandPartnerId) => Future[Option[UserList]])(implicit ctx: ExecutionContext):
  (UserListName, DemandPartnerId) => Future[Option[UserList]] = { (name, dpId) =>
      getUserList(name, dpId).map(_.map(
        result => UserList(
          result.id,
          result.name,
          result.dpId,
          result.created,
          result.modified
        )))
  }

  def getUserLists(
    getUserLists: (Option[DemandPartnerId], Offset, Limit) =>
      Future[List[UserList]])(implicit ctx: ExecutionContext):
  (Option[DemandPartnerId], Offset, Limit) =>
    Future[List[UserList]] =
    (dpId: Option[DemandPartnerId], o: Offset, l: Limit) =>

      getUserLists(dpId, o, l)
}
