package com.kuzhagulov.jobsboard.http

import cats.*
import cats.effect.Concurrent
import cats.implicits.*
import com.kuzhagulov.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.*
import org.http4s.server.Router

class HttpApi[F[_] : Concurrent] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F].routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_] : Concurrent] = new HttpApi[F]
}
