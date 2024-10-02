package com.kuzhagulov.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.kuzhagulov.jobsboard.domain.job.*

import java.util.UUID

trait Jobs[F[_]] {
  // algebra
  // CRUD
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]: MonadCancelThrow] private(xa: Transactor[F]) extends Jobs[F] {
  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
         INSERT INTO jobs(
            date,
            ownerEmail,
            company,
            title,
            description,
            externalUrl,
            remote,
            salaryLo,
            salaryHi,
            currency,
            location,
            country,
            tags,
            seniority,
            other,
            active
         ) VALUES (
            ${System.currentTimeMillis()},
            $ownerEmail,
            ${jobInfo.company},
            ${jobInfo.title},
            ${jobInfo.description},
            ${jobInfo.externalUrl},
            ${jobInfo.remote},
            ${jobInfo.salaryLo},
            ${jobInfo.salaryHi},
            ${jobInfo.currency},
            ${jobInfo.location},
            ${jobInfo.country},
            ${jobInfo.tags},
            ${jobInfo.seniority},
            ${jobInfo.other},
            false
         )"""
      .update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    sql"""SELECT
              id,
              date,
              ownerEmail,
              company,
              title,
              description,
              externalUrl,
              remote,
              salaryLo,
              salaryHi,
              currency,
              location,
              country,
              tags,
              seniority,
              other,
              active
            FROM
              jobs"""
      .query[Job]
      .to[List]
      .transact(xa)

  override def find(id: UUID): F[Option[Job]] =
    sql"""SELECT
                id,
                date,
                ownerEmail,
                company,
                title,
                description,
                externalUrl,
                remote,
                salaryLo,
                salaryHi,
                currency,
                location,
                country,
                tags,
                seniority,
                other,
                active
              FROM
                jobs
              WHERE id = $id"""
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
         UPDATE jobs
         SET
            company = ${jobInfo.company},
            title = ${jobInfo.title},
            description = ${jobInfo.description},
            externalUrl = ${jobInfo.externalUrl},
            remote = ${jobInfo.remote},
            salaryLo = ${jobInfo.salaryLo},
            salaryHi = ${jobInfo.salaryHi},
            currency = ${jobInfo.currency},
            location = ${jobInfo.location},
            country = ${jobInfo.country},
            tags = ${jobInfo.tags},
            seniority = ${jobInfo.seniority},
            other = ${jobInfo.other}
         WHERE id = $id"""
      .update
      .run
      .transact(xa)
      .flatMap(_ => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
          DELETE FROM jobs
          WHERE id = $id
         """
      .update
      .run
      .transact(xa)
}

object LiveJobs {
  given jobRead: Read[Job] = Read[(
    UUID,
      Long,
      String,
      String,
      String,
      String,
      String,
      Boolean,
      Option[Int],
      Option[Int],
      Option[String],
      String,
      Option[String],
      Option[List[String]],
      Option[String],
      Option[String],
      Boolean
    )].map {
    case(
      id: UUID,
      date: Long,
      ownerEmail: String,
      company: String,
      title: String,
      description: String,
      externalUrl: String,
      remote: Boolean,
      salaryLo: Option[Int] @unchecked,
      salaryHi: Option[Int] @unchecked,
      currency: Option[String] @unchecked,
      location: String,
      country: Option[String] @unchecked,
      tags: Option[List[String]] @unchecked,
      seniority: Option[String] @unchecked,
      other: Option[String] @unchecked,
      active: Boolean
      ) =>
      Job(
        id = id,
        date = date,
        ownerEmail = ownerEmail,
        JobInfo(
          company = company,
          title = title,
          description = description,
          externalUrl = externalUrl,
          remote = remote,
          salaryLo = salaryLo,
          salaryHi = salaryHi,
          currency = currency,
          location = location,
          country = country,
          tags = tags,
          seniority = seniority,
          other = other,
        ),
        active = active
      )
  }
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
}
