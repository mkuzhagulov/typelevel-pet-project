package com.kuzhagulov.jobsboard.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.derivation.default.*

// generates given configReader: ConfigReader[EmberConfig]
final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostString =>
    Host.fromString(hostString) match {
      case Some(host) => Right(host)
      case None => Left(CannotConvert(hostString, Host.getClass.toString, s"Invalid host string: $hostString"))
    }
  }
  
  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt => 
    Port.fromInt(portInt) match {
      case Some(port) => Right(port)
      case None => Left(CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port int: $portInt"))
    }
  }
}
