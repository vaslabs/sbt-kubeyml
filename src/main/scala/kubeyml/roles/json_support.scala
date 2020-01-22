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

package kubeyml.roles

import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
object json_support {
  import kubeyml.protocol.json_support._

  implicit val verbsEncoder: Encoder[Verb] = Encoder.encodeString.contramap {
    case Verb.Get => "get"
    case Verb.Watch => "watch"
    case Verb.List => "list"
  }
  implicit val apiGroupEncoder: Encoder[ApiGroup] = Encoder.encodeString.contramap {
    case ApiGroup.Core => ""
  }
  implicit val resourceEncoder: Encoder[Resource] = Encoder.encodeString.contramap {
    case Pods => "pods"
  }

  implicit val roleEncoder: Encoder[Role] = Encoder.instance {
    case role =>
      Json.obj("metadata" -> role.metadata.asJson,
        "rules" -> role.rules.asJson,
        "kind" -> "Role".asJson,
        "apiVersion" -> "rbac.authorization.k8s.io/v1".asJson
      )
  }

  implicit val roleRefEncoder: Encoder[RoleRef] = Encoder.instance {
    case RoleRef(Role(RoleMetadata(name, _), _)) =>
      Json.obj(
        "kind" -> "Role".asJson,
        "name" -> name.asJson,
        "apiGroup" -> "rbac.authorization.k8s.io".asJson
      )
  }

  implicit val subjectEncoder: Encoder[Subject] = Encoder.instance {
    case UserSubject(serviceAccount, namespace) =>
      val identifier = s"system:serviceaccount:${namespace.value}:${serviceAccount.value}"
      Json.obj(
        "kind" -> "User".asJson,
        "name" -> identifier.asJson
      )
  }

  implicit val roleBindingEncoder: Encoder[RoleBinding] = Encoder.instance {
    case RoleBinding(metadata, subjects, roleRef) =>
      Json.obj(
        "kind" -> "RoleBinding".asJson,
        "apiVersion" -> "rbac.authorization.k8s.io/v1".asJson,
        "metadata" -> metadata.asJson,
        "subjects" -> subjects.asJson,
        "roleRef" -> roleRef.asJson
      )
  }

}
