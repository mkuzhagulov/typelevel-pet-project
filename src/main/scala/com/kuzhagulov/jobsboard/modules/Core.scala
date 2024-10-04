package com.kuzhagulov.jobsboard.modules

import cats.effect.*
import cats.effect.implicits.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import com.kuzhagulov.jobsboard.core.{Jobs, LiveJobs}
import doobie.util.transactor.Transactor

final class Core[F[_]] private (val jobs: Jobs[F])

// postgres -> jobs -> core -> app
object Core {
  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] = {
    Resource
      .eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
  }
}