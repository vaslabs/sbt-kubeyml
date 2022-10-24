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

package kubeyml.service

import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.circe.generic.semiauto._
import kubeyml.protocol.json_support._

object json_support {

  implicit val serviceTypeEncoder: Encoder[ServiceType] = Encoder.encodeString.contramap {
    case NodePort => "NodePort"
  }

  implicit val selectorEncoder: Encoder[Selector] = Encoder.instance {
    case AppSelector(appName) =>
      Json.obj("app" -> appName.asJson)
  }

  implicit val networkProtocolEncoder: Encoder[NetworkProtocol] = Encoder.encodeString.contramap {
    case TCP => "TCP"
  }

  implicit val targetPortEncoder: Encoder[TargetPort] = Encoder.instance {
    case NamedTargetPort(name) =>
      name.asJson
    case NumberedTargetPort(num) =>
      num.asJson
  }

  implicit val portEncoder: Encoder[Port] = deriveEncoder

  implicit val specEncoder: Encoder[Spec] = deriveEncoder

  implicit val serviceEncoder: Encoder[Service] = Encoder.instance { service =>
    Json.obj(
      "kind" -> "Service".asJson,
      "apiVersion" -> "v1".asJson,
      "metadata" -> Json.obj(
        "name" -> service.name.asJson,
        "namespace" -> service.namespace.asJson
      ),
      "spec" -> service.spec.asJson
    )
  }
}
