/*
 * Copyright (c) 2019 Vasilis Nicolaou
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kubeyml.deployment

import kubeyml.protocol.json_support._
import io.circe.{Encoder, Json}

object json_support {

  import io.circe.generic.auto._
  import io.circe.generic.semiauto._
  import io.circe.syntax._

  implicit val rollingUpdateEncoder: Encoder[RollingUpdate] = deriveEncoder[RollingUpdate].mapJson { json =>
    Json.obj(
      "type" -> "RollingUpdate".asJson,
      "rollingUpdate" -> json
    )
  }

  private def removeNulls[A](encoder: Encoder.AsObject[A]) =
    encoder.mapJsonObject(_.filter { case (_, v) => !v.isNull && v.asArray.forall(_.nonEmpty) })

  implicit val deploymentStrategyEncoder: Encoder[DeploymentStrategy] = Encoder.instance {
    case r: RollingUpdate => rollingUpdateEncoder.apply(r)
    case Recreate         => Json.obj("type" -> "Recreate".asJson)
  }

  implicit val containerCmdEncoder: Encoder[Command] = Encoder.instance { case Command(cmd) =>
    Seq(cmd).asJson
  }

  implicit val imagePullPolicyEncoder: Encoder[ImagePullPolicy] = Encoder.instance {
    case Always       => "Always".asJson
    case IfNotPresent => "IfNotPresent".asJson
  }

  implicit val httpGetEncoder: Encoder[HttpGet] = removeNulls(deriveEncoder)

  implicit val httpProbeEncoder: Encoder[HttpProbe] = Encoder.instance[HttpProbe] { httpProbe =>
    {
      val fields: List[(String, Json)] = List(
        "httpGet" -> httpProbe.httpGet.asJson,
        "initialDelaySeconds" -> httpProbe.initialDelay.toSeconds.asJson,
        "periodSeconds" -> httpProbe.period.toSeconds.asJson,
        "timeoutSeconds" -> httpProbe.timeout.toSeconds.asJson,
        "failureThreshold" -> httpProbe.failureThreshold.asJson,
        "successThreshold" -> httpProbe.successThreshold.asJson
      )

      Json.obj(
        fields: _*
      )
    }
  }

  implicit val portCodec: Encoder[Port] = removeNulls(deriveEncoder[Port])

  implicit val cpuEncoder: Encoder[Cpu] = Encoder.encodeString.contramap(_.value.toString + 'm')
  implicit val memoryEncoder: Encoder[Memory] = Encoder.encodeString.contramap(_.value.toString + "Mi")

  implicit val probeEncoder: Encoder[Probe] = Encoder.instance {
    case h: HttpProbe => httpProbeEncoder(h)
    case NoProbe      => Json.Null
  }

  implicit val containerEncoder: Encoder[Container] = removeNulls(deriveEncoder[Container])

  implicit val deploymentEncoder: Encoder[Deployment] = deriveEncoder[Deployment].mapJsonObject(
    _.+:("kind" -> "Deployment".asJson)
  )

  private def keyValue(value: EnvValue): String = value match {
    case EnvRawValue(_) =>
      "value"
    case EnvFieldValue(_) =>
      "valueFrom"
    case EnvSecretValue(_, _) =>
      "valueFrom"
  }

  private val envFieldValueEncoder: Encoder[EnvFieldValue] = Encoder.instance { fieldValue =>
    Json.obj(
      "fieldRef" -> Json.obj("fieldPath" -> fieldValue.fieldPath.asJson)
    )
  }

  private val envSecretValueEncoder: Encoder[EnvSecretValue] = Encoder.instance { secretValue =>
    Json.obj(
      "secretKeyRef" -> secretValue.asJson
    )
  }

  private val envRawValueEncoder: Encoder[EnvRawValue] = Encoder.encodeString.contramap(_.value)

  implicit val envValueEncoder: Encoder[EnvValue] = Encoder.instance {
    case raw: EnvRawValue         => envRawValueEncoder(raw)
    case secret: EnvSecretValue   => envSecretValueEncoder(secret)
    case fieldPath: EnvFieldValue => envFieldValueEncoder(fieldPath)
  }

  implicit val envVarDefinitionEncoder: Encoder[EnvVarDefinition] = Encoder.instance { envVarDefinition =>
    Json.obj(
      "name" -> envVarDefinition.name.asJson,
      keyValue(envVarDefinition.value) -> envVarDefinition.value.asJson
    )
  }

  implicit val environmentVariablesEncoder: Encoder[Map[EnvName, EnvValue]] = Encoder
    .encodeList[EnvVarDefinition]
    .contramap(_.toList.map { case (name, value) =>
      EnvVarDefinition(name.value, value)
    })

  implicit val templateSpecEncoder: Encoder[TemplateSpec] = Encoder.instance(ts =>
    Json.obj(
      Seq(
        "containers" -> ts.containers.asJson,
        "volumes" -> ts.volumes.asJson
      ) ++ (addIfNonEmpty(ts.hostAliases, "hostAliases") ++ addIfNonEmpty(ts.volumes, "volumes")): _*
    )
  )

  private def addIfNonEmpty[A](aliases: List[A], key: String)(implicit encoder: Encoder[A]): Seq[(String, Json)] =
    if (aliases.isEmpty)
      Seq.empty
    else
      Seq(
        key -> aliases.asJson
      )
}
