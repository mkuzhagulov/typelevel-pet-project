package com.kuzhagulov.foundation

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

import java.util.UUID

object Http4s extends IOApp.Simple {
  type Student = String

  case class Instructor(firstName: String, lastName: String)

  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {
    val catsEffectCourse = Course(
      "2976c2f3-e769-4edc-8fd1-e105c1a29cf2",
      "Rock the JVM",
      2024,
      List("Marat", "Alice"),
      "Martin Odersky"
    )

    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    // API
    def findCourseById(id: UUID): Option[Course] =
      courses.get(id.toString)

    def findCourseByInstructor(name: String): List[Course] = {
      courses.values.filter(_.instructorName == name).toList
    }
  }

  /*
    REST Endpoints:
    GET localhost:8080/courses?instructor=Martin%20Odersky&year=2024
    GET localhost:8080/courses/2976c2f3-e769-4edc-8fd1-e105c1a29cf2/students
  */

  object InstructorQueryParameterMatcher extends QueryParamDecoderMatcher[String]("instructor")

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParameterMatcher(instructor) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCourseByInstructor(instructor)
        maybeYear match {
          case Some(y) => y.fold(
            _ => BadRequest("Parameter 'year' is invalid"),
            year =>
              val res = courses.filter(_.year == year)
              Ok(res.asJson)
          )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCourseById(courseId).map(_.students) match {
          case Some(students) => Ok(students.asJson)
          case None => NotFound(s"No course with $courseId was found.")
        }
    }
  }

  def healthEndpoint[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All going great!")
    }
  }

  def allRoutes[F[_] : Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHttpApp(routerWithPathPrefixes)
      .build
      .use(_ => IO.println("Server started...") *> IO.never)
}
