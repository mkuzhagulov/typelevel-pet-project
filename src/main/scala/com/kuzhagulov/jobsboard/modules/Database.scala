package com.kuzhagulov.jobsboard.modules

import cats.effect.*
import cats.effect.implicits.*
import com.kuzhagulov.jobsboard.config.PostgresConfig
import doobie.hikari.HikariTransactor
import doobie.util.*

object Database {
  def makePostgresResource[F[_] : Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver", // TODO move to config
      config.url,
      config.user,
      config.password,
      ec
    )
  } yield xa
}
