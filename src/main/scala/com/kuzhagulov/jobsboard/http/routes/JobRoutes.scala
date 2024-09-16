package com.kuzhagulov.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.{Concurrent, IO, IOApp}
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger


import com.kuzhagulov.jobsboard.domain.job.*
import com.kuzhagulov.jobsboard.http.responses.FailureResponse
import com.kuzhagulov.jobsboard.logging.syntax.*
import java.util.UUID
import scala.collection.mutable.Map as MutableMap

class JobRoutes[F[_] : Concurrent : Logger] private extends Http4sDsl[F] {

  // "database"
  private val database = MutableMap.empty[UUID, Job]

  // POST /jobs?offset=x&limit=y + { filters } TODO: later
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root =>
      Ok(database.values)
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) => Ok(job)
        case None => NotFound(FailureResponse(s"Job not found by $id"))
      }
  }

  // POST /jobs/create { jobInfo }
  private def createJob(jobInfo: JobInfo): F[Job] = {
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "test@example.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]
  }
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
        job <- createJob(jobInfo)
        _ <- database.put(job.id, job).pure[F]
        resp <- Created(job.id)
      } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _ <- database.update(id, job.copy(jobInfo = jobInfo)).pure[F]
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot update job $id: not found"))
      }
  }

  // DELETE /jobs/uuid
  private def deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            _ <- database.remove(id).pure[F]
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
      }
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_] : Concurrent : Logger] = new JobRoutes[F]
}
