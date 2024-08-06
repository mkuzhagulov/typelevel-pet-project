package com.kuzhagulov.jobsboard.domain

import java.util.UUID

object job {
  case class Job(
    id: UUID,
    date: Long,
    ownerEmail: String,
    jobInfo: JobInfo,
    active: Boolean = false)

  case class JobInfo(
    company: String,
    title: String,
    description: String,
    externalUrl: String,
    remote: Boolean,
    salaryLo: Option[Int],
    salaryHi: Option[Int],
    location: String,
    country: Option[String],
    tags: Option[List[String]],
    seniority: Option[String],
    other: Option[String])
  
  object JobInfo {
    val empty = JobInfo("", "", "", "", false, None, None, "", None, None, None, None)
  }
}
