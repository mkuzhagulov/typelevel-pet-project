package com.kuzhagulov.jobsboard.http.routes

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router

class HealthRoutes[F[_] : Monad] private extends Http4sDsl[F] {
  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok("All going great!")
  }

  val routes = Router(
    "/health" -> healthRoute
  )
}

object HealthRoutes {
  def apply[F[_] : Monad] = new HealthRoutes[F]
}
