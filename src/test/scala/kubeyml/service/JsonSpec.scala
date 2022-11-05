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

import io.circe.Json
import io.circe.yaml.parser._
import io.circe.syntax._
import kubeyml.protocol.{NonEmptyString, PortNumber}
import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import kubeyml.service.json_support._

class JsonSpec extends Properties("service") {

  property("validdefinition") = Prop.forAll(JsonSpec.arbitraryServiceVariablesNoEmpty) { serviceVariables =>
    val generated = Service(
      serviceVariables.name,
      serviceVariables.namespace,
      Spec(
        serviceVariables.`type`,
        AppSelector(serviceVariables.appSelector),
        serviceVariables.ports.map { portVar =>
          Port(portVar.name, portVar.protocol, PortNumber(portVar.port), NumberedTargetPort(PortNumber(portVar.targetPort)))
        }
      )
    ).asJson
    val expected = JsonSpec.generateYaml(serviceVariables)

    if (generated != expected) {
      println(generated)
      println(expected)
    }
    generated == expected
  }

}

object JsonSpec {

  case class PortVariable(
    name: NonEmptyString,
    protocol: NetworkProtocol,
    port: Int,
    targetPort: Int
  )

  case class ServiceVariables(
    name: NonEmptyString,
    namespace: NonEmptyString,
    `type`: ServiceType,
    appSelector: NonEmptyString,
    ports: List[PortVariable]
  )

  implicit lazy val arbitraryNonEmptyString: Arbitrary[NonEmptyString] =
    Arbitrary(Gen.alphaStr.filterNot(_.isEmpty).map(NonEmptyString))

  implicit lazy val arbitraryPortVariable: Arbitrary[PortVariable] = Arbitrary(
    for {
      port <- Gen.chooseNum[Int](0, 65535)
      targetPort <- Gen.chooseNum[Int](0, 65535)
      protocol <- Gen.const(TCP)
      name <- arbitraryNonEmptyString.arbitrary
    } yield PortVariable(name, protocol, port, targetPort)
  )

  implicit lazy val arbitraryServiceVariablesNoEmpty: Gen[ServiceVariables] = for {
    name <- arbitraryNonEmptyString.arbitrary
    namespace <- arbitraryNonEmptyString.arbitrary
    serviceType <- Gen.const(NodePort)
    appSelector <- arbitraryNonEmptyString.arbitrary
    ports <- Gen.listOf(arbitraryPortVariable.arbitrary)
  } yield ServiceVariables(name, namespace, serviceType, appSelector, ports)

  def canonicalJson(networkProtocol: NetworkProtocol): Json = networkProtocol match {
    case TCP => Json.fromString("TCP")
  }

  def canonicalJson(portVariable: PortVariable): Json =
    Json.obj(
      "name" -> Json.fromString(portVariable.name.value),
      "protocol" -> canonicalJson(portVariable.protocol),
      "port" -> Json.fromInt(portVariable.port),
      "targetPort" -> Json.fromInt(portVariable.targetPort)
    )

  def generateYaml(serviceVariables: ServiceVariables): Json = {
    val ymlString =
      s"""
        |kind: Service
        |apiVersion: v1
        |metadata:
        |  name: "${serviceVariables.name.value}"
        |  namespace: "${serviceVariables.namespace.value}"
        |spec:
        |  type: ${serviceVariables.`type`}
        |  selector:
        |    app: "${serviceVariables.appSelector.value}"
        |""".stripMargin
    val json = parse(ymlString).right.get

    val portsJson = Json.obj(
      "spec" -> Json.obj("ports" -> serviceVariables.ports.map(canonicalJson).asJson)
    )

    json.deepMerge(portsJson)

  }

}
