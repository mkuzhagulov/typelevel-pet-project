package com.kuzhagulov.jobsboard

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import com.kuzhagulov.jobsboard.config.EmberConfig
import com.kuzhagulov.jobsboard.http.routes.HealthRoutes
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource

object Application extends IOApp.Simple {

  val configSource: Result[EmberConfig] = ConfigSource.default.load[EmberConfig]

  override def run: IO[Unit] =
    configSource match {
      case Right(conf) =>
        EmberServerBuilder
          .default[IO]
          .withHost(conf.host)
          .withPort(conf.port)
          .withHttpApp(HealthRoutes[IO].routes.orNotFound)
          .build
          .use(_ => IO.println("Server started...") *> IO.never)
      case Left(ex) =>
    }


}
