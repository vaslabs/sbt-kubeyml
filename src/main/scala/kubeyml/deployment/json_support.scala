package kubeyml.deployment

import io.circe.{Encoder, Json, ObjectEncoder}

object json_support {

  import io.circe.generic.auto._
  import io.circe.generic.semiauto._
  import io.circe.syntax._

  implicit val rollingUpdateEncoder: Encoder[RollingUpdate] = deriveEncoder[RollingUpdate].mapJson {
    json =>
      Json.obj(
        "type" -> "RollingUpdate".asJson,
        "rollingUpdate" -> json
      )
  }

  private def removeNulls[A](encoder: ObjectEncoder[A]) =
    encoder.mapJsonObject(_.filter { case (_, v) => !v.isNull && v.asArray.map(_.size > 0).getOrElse(true) })

  implicit val deploymentStrategyEncoder: Encoder[DeploymentStrategy] = Encoder.instance {
      case r: RollingUpdate => rollingUpdateEncoder.apply(r)
    }



  implicit val imagePullPolicyEncoder: Encoder[ImagePullPolicy] = Encoder.instance {
    case Always => "Always".asJson
    case IfNotPresent => "IfNotPresent".asJson
  }

  implicit val httpGetEncoder: Encoder[HttpGet] = removeNulls(deriveEncoder)

  implicit val httpProbeEncoder: Encoder[HttpProbe] = Encoder.instance[HttpProbe] {
    httpProbe => {
      val fields: List[(String, Json)] = List("httpGet" -> httpProbe.httpGet.asJson,
        "initialDelaySeconds" -> httpProbe.initialDelay.toSeconds.asJson,
        "periodSeconds" -> httpProbe.period.toSeconds.asJson
      ) ++ httpProbe.failureThreshold.map("failureThreshold" -> _.asJson).toList

      Json.obj(
        fields: _*
      )
    }
  }

  implicit val probeEncoder: Encoder[Probe] = Encoder.instance {
    case h: HttpProbe => httpProbeEncoder(h)
    case NoProbe => Json.Null
  }

  implicit val containerEncoder: Encoder[Container] = removeNulls(deriveEncoder[Container])

  implicit val deploymentEncoder: Encoder[Deployment] = deriveEncoder[Deployment].mapJsonObject(
    _.+:("kind" -> "Deployment".asJson)
  )

}
