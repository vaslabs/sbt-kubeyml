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

import kubeyml.protocol.{NonEmptyString, PortNumber}


case class Service(
    name: NonEmptyString,
    namespace: NonEmptyString,
    spec: Spec
)

case class Spec(
  `type`: ServiceType,
  selector: Selector,
  ports: List[Port]
)


sealed trait ServiceType

case object NodePort extends ServiceType

sealed trait Selector
case class AppSelector(appName: NonEmptyString) extends Selector


case class Port(
     name: NonEmptyString,
     protocol: NetworkProtocol,
     port: PortNumber,
     targetPort: TargetPort
)

sealed trait NetworkProtocol
case object TCP extends NetworkProtocol


sealed trait TargetPort
case class NamedTargetPort(name: NonEmptyString) extends TargetPort
case class NumberedTargetPort(value: PortNumber) extends TargetPort