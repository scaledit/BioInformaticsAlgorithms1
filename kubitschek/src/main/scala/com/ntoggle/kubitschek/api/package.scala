package com.ntoggle.kubitschek

import akka.http.scaladsl.server.Rejection
import scala.concurrent.Future
import scalaz.{EitherT, \/}

package object api {

  type ApiResponse[A] = Rejection \/ A
  type ApiResponseT[F[_], A] = EitherT[F, Rejection, A]
  type FutureApiResponse[A] = Future[Rejection \/ A]
  type ApiResponseFuture[A] = EitherT[Future, Rejection, A]

}
