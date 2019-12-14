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

import kubeyml.protocol.{NonEmptyString, PortNumber}

sealed trait Ingress

case class CustomIngress(name: NonEmptyString, namespace: NonEmptyString, annotations: Map[NonEmptyString, String], spec: Spec)
    extends Ingress

case class Spec(rules: List[Rule])

sealed trait Rule

case class Host(value: String) {
  require(value.nonEmpty, "Hostname cannot be empty")
  require(value.matches("([a-zA-Z0-9\\-_]+\\.?)*"), s"Hostname has a wrong format ${value}")
}

case class HttpRule(host: Host, paths: List[Path]) extends Rule

case class Path(serviceMapping: ServiceMapping, value: NonEmptyString) {
  require(java.nio.file.Paths.get(value.value).toAbsolutePath.toString == value.value, s"Not a valid path ${value.value}")
}

case class ServiceMapping(serviceName: NonEmptyString, servicePort: PortNumber)
