package kubeyml.helm

import io.circe.{Encoder, Json}
import io.circe.syntax._
import kubeyml.protocol.json_support._

object json_support {

  implicit val chartEncoder: Encoder[Chart] = Encoder.instance(
    c =>
      Json.obj(
        "apiVersion" -> "v2".asJson,
        "name" -> c.name.asJson,
        "version" -> c.name.asJson
      )
  )
}
