package com.kuzhagulov.jobsboard.playground

import cats.effect.*
import com.kuzhagulov.jobsboard.core.LiveJobs
import com.kuzhagulov.jobsboard.domain.job.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.*

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    "Google",
    "Senior .NET Developer",
    "Senior .NET developer with minimum 6 years of experience",
    "hr.google.com",
    false,
    "US"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs            <- LiveJobs[IO](xa)
      _               <- IO.println("Ready. Next...") *> IO(StdIn.readLine())
      id              <- jobs.create("marat.kuz@gmail.com", jobInfo)
      _               <- IO.println("Next...") *> IO(StdIn.readLine())
      list            <- jobs.all()
      _               <- IO.println(s"All jobs: $list") *> IO(StdIn.readLine())
      _               <- jobs.update(id, jobInfo.copy(company = "Yahoo"))
      newJob          <- jobs.find(id)
      _               <- IO.println(s"New Job: $newJob") *> IO(StdIn.readLine())
      _               <- jobs.delete(id)
      listAfterDelete <- jobs.all()
      _               <- IO.println(s"All jobs after delete: $listAfterDelete") *> IO(StdIn.readLine())
    } yield ()
  }
}
