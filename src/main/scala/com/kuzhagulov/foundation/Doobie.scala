package com.kuzhagulov.foundation

import cats.effect.kernel.MonadCancelThrow
import cats.effect.{IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor

object Doobie extends IOApp.Simple {

  case class Student(id: Int, name: String)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // JDBC Connector
    "jdbc:postgresql://localhost:5432/demo", // database URL
    "docker", // user
    "docker" // password
  )

  def findAllStudents: IO[List[String]] = {
    val action = sql"SELECT name FROM students".query[String].to[List]
    action.transact(xa)
  }

  def saveStudent(id: Int, name: String): IO[Int] = {
    val action = sql"INSERT INTO students VALUES($id, $name)".update.run
    action.transact(xa)
  }

  def findStudentsByInitial(letter: String): IO[List[Student]] = {
    val selectPart = fr"SELECT id, name"
    val fromPart = fr"FROM students"
    val wherePart = fr"WHERE LEFT(name, 1) = $letter"

    val wholeStatement = selectPart ++ fromPart ++ wherePart
    val action = wholeStatement.query[Student].to[List]
    action.transact(xa)
  }

  // Tagless Final approach

  trait Students[F[_]] {
    def findById(id: Int): F[Option[Student]]

    def findAll: F[List[Student]]

    def create(name: String): F[Int]
  }

  object Students {
    def make[F[_] : MonadCancelThrow](xa: Transactor[F]) = new Students[F] {

      override def findById(id: Int): F[Option[Student]] =
        sql"SELECT id, name FROM students WHERE id = $id".query[Student].option.transact(xa)

      override def findAll: F[List[Student]] =
        sql"SELECT id, name FROM students".query[Student].to[List].transact(xa)

      override def create(name: String): F[Int] =
        sql"INSERT INTO students(name) values ($name)".update.withUniqueGeneratedKeys[Int]("id").transact(xa)
    }
  }

  val postgresResource = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/demo",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val smallProgram = postgresResource.use { xa =>
    val studentsRepo = Students.make[IO](xa)

    for {
      id <- studentsRepo.create("Marat")
      marat <- studentsRepo.findById(id)
      _ <- IO.println(s"The Senior Scala Developer of SBER: $marat")
    } yield ()
  }

  override def run: IO[Unit] = smallProgram
}
