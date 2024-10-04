package com.kuzhagulov.jobsboard.modules

import cats.*
import cats.effect.Concurrent
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import com.kuzhagulov.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class HttpApi[F[_] : Concurrent : Logger] private (core: Core[F]) {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F](core.jobs).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger](core: Core[F]): Resource[F, HttpApi[F]] = Resource.pure(new HttpApi[F](core))
}
