package com.kuzhagulov.jobsboard.http.routes

import cats.*
import cats.effect.{ Concurrent, IO, IOApp }
import cats.implicits.*
import com.kuzhagulov.jobsboard.core.*
import com.kuzhagulov.jobsboard.domain.job.*
import com.kuzhagulov.jobsboard.http.responses.FailureResponse
import com.kuzhagulov.jobsboard.logging.syntax.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.collection.mutable.Map as MutableMap

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends Http4sDsl[F] {

  // POST /jobs?offset=x&limit=y + { filters } TODO: later
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    for {
      jobsList <- jobs.all()
      resp     <- Ok(jobsList)
    } yield resp
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job not found by $id"))
    }
  }

  // POST /jobs/create { jobInfo }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "create" =>
    for {
      jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
      jobId   <- jobs.create("TODO@example.com", jobInfo)
      resp    <- Created(jobId)
    } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / UUIDVar(id) =>
    for {
      jobInfo   <- req.as[JobInfo]
      newJobOpt <- jobs.update(id, jobInfo)
      resp      <- newJobOpt match {
        case Some(job) => Ok()
        case None      => NotFound(FailureResponse(s"Cannot update job $id: not found"))
      }
    } yield resp
  }

  // DELETE /jobs/uuid
  private def deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) =>
        for {
          _    <- jobs.delete(id)
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
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
