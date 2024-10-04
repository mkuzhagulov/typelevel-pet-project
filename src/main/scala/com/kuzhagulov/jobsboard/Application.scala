package com.kuzhagulov.jobsboard

import cats.*
import cats.effect.{IO, IOApp}
import cats.implicits.*
import com.kuzhagulov.jobsboard.config.{AppConfig, EmberConfig}
import com.kuzhagulov.jobsboard.config.syntax.*
import com.kuzhagulov.jobsboard.modules.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap { config =>
    val appResource = for {
      xa      <- Database.makePostgresResource[IO](config.postgresConfig)
      core    <- Core[IO](xa)
      httpApi <- HttpApi[IO](core)
      server  <- EmberServerBuilder
        .default[IO]
        .withHost(config.emberConfig.host)
        .withPort(config.emberConfig.port)
        .withHttpApp(httpApi.endpoints.orNotFound)
        .build
    } yield server

    appResource.use(_ => IO.println("Server started...") *> IO.never)
  }
}
