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

package kubeyml.ingress

import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import kubeyml.protocol.json_support._

object json_support {

  private val defaultIngressParts: Json = Json.obj(
    "kind" -> "Ingress".asJson
  )

  implicit val apiVersionEncoder: Encoder[ApiVersion] = Encoder.instance(_.show.asJson)

  implicit val pathEncoder: Encoder[Path] = Encoder.instance {
    case Path(ServiceMapping(serviceName, servicePort), path) =>
      Json.obj(
        "backend" -> Json.obj(
          "serviceName" -> serviceName.asJson,
          "servicePort" -> servicePort.asJson
        ),
        "path" -> path.asJson
      )
  }

  private val httpRuleEncoder: Encoder[HttpRule] = Encoder.instance { httpRule =>
    Json.obj(
      "host" -> httpRule.host.asJson,
      "http" -> Json.obj("paths" -> httpRule.paths.asJson)
    )
  }

  implicit val ruleEncoder: Encoder[Rule] = Encoder.instance {
    case r: HttpRule => httpRuleEncoder(r)
  }

  private val customIngressEncoder: Encoder[CustomIngress] = Encoder.instance(
    custom =>
      Json.obj(
        "apiVersion" -> custom.apiVersion.asJson,
        "metadata" -> Json.obj(
          "annotations" -> custom.annotations.asJson,
          "name" -> custom.name.asJson,
          "namespace" -> custom.namespace.asJson
        ),
        "spec" -> custom.spec.asJson
      )
  )

  implicit val ingressEncoder: Encoder[Ingress] = Encoder.instance {
    case custom: CustomIngress =>
      defaultIngressParts deepMerge customIngressEncoder(custom)
  }

}
