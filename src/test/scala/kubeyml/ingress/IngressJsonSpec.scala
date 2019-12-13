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

import io.circe.{Json, ParsingFailure}
import org.scalacheck._
import io.circe.yaml.parser._
import kubeyml.deployment.api._
import io.circe.syntax._
import cats.implicits._
import kubeyml.protocol.NonEmptyString
import json_support._

class IngressJsonSpec extends Properties("ingress"){
  import IngressJsonSpec._

  property("valid definitions")) = Prop.forAll(IngressJsonSpec.validDefinitionsGen) {
    valid =>
      val expectedJson = ingress(valid).right.get

      val httpRules = valid.rules.map {
       case ruleVariable =>
          val paths = ruleVariable.paths.map(p => Path(ServiceMapping(p.serviceName, p.port), p.value))
          HttpRule(Host(ruleVariable.host), paths)
      }

      val nonEmptyAnnotations: Map[NonEmptyString, NonEmptyString] = valid.annotations.map {
        case (key, value) => NonEmptyString(key) -> NonEmptyString(value)
      }
      val ingressDef: Ingress = CustomIngress(valid.name, valid.namespace,nonEmptyAnnotations, Spec(httpRules))
      val generatedJson = ingressDef.asJson
      if (generatedJson != expectedJson) {
        println(generatedJson)
        println(expectedJson)
      }

      generatedJson == expectedJson

  }


}


object IngressJsonSpec {

  case class PathVariable(serviceName: String, port: Int, value: String)

  case class RuleVariable(host: String, paths: List[PathVariable])

  def pathVariable(pathVariable: PathVariable) = {
    parse(s"""
      |backend:
      |  serviceName: "${pathVariable.serviceName}"
      |  servicePort: ${pathVariable.port}
      |path: "${pathVariable.value}"
      |""".stripMargin)
  }

  def rule(ruleVariable: RuleVariable) = {
    val template = parse(
      s"""
        |host: "${ruleVariable.host}"
        |http:
        |  paths:
        |""".stripMargin
    )
    val ruleVariablesJson: Either[ParsingFailure, List[Json]] =
      ruleVariable.paths.map(pathVariable).sequence
    for {
      pathVariables <- ruleVariablesJson
      ruleVariable <- template
      nesting = Json.obj("http" -> Json.obj("paths" -> pathVariables.asJson))
    } yield ruleVariable deepMerge nesting
  }

  def ingress(
               annotations: Map[String, String],
               name: String,
               namespace: String,
               rules: List[RuleVariable]): Either[ParsingFailure, Json] = {
    val jsonDefinition = parse(s"""
       |apiVersion: extensions/v1beta1
       |kind: Ingress
       |metadata:
       |  name: "${name}"
       |  namespace: "${namespace}"
       |""".stripMargin)

    val rulesJson = rules.map(rule).sequence.map(_.asJson)
    val nestedRules = rulesJson.map(rulesJs => Json.obj("spec" -> Json.obj("rules" -> rulesJs)))
    val nestedAnnotations = Json.obj("metadata" -> Json.obj("annotations" -> annotations.asJson))

    for {
      template <- jsonDefinition
      nestedRules <- nestedRules
    } yield template deepMerge nestedRules deepMerge nestedAnnotations
  }

  def ingress(validDefinitions: ValidDefinitions): Either[ParsingFailure, Json] = ingress(
    validDefinitions.annotations, validDefinitions.name,
    validDefinitions.namespace, validDefinitions.rules
  )

  case class ValidDefinitions(annotations: Map[String, String], name: String, namespace: String, rules: List[RuleVariable])

  implicit lazy val arbitraryNonEmptyString: Arbitrary[NonEmptyString] =
    Arbitrary(Gen.alphaStr.filterNot(_.isEmpty).map(NonEmptyString))

  private val pathVariableGen: Gen[PathVariable] = for {
    serviceName <- arbitraryNonEmptyString.arbitrary.map(_.value)
    servicePort <- Gen.chooseNum(0, 65535)
    path <- Gen.oneOf(arbitraryNonEmptyString.arbitrary.map(s => s"/$s"), Gen.const("/"))
  } yield PathVariable(serviceName, servicePort, path)

  private lazy val ruleVariableGen: Gen[RuleVariable] = for {
    host <- Gen.oneOf(
      Gen.listOfN(2, arbitraryNonEmptyString.arbitrary.map(_.value)).map(_.mkString(".")),
      Gen.listOfN(3, arbitraryNonEmptyString.arbitrary.map(_.value)).map(_.mkString(".")),
      Gen.listOfN(4, arbitraryNonEmptyString.arbitrary.map(_.value)).map(_.mkString(".")),
      Gen.const("localhost")
    )
    pathVariable <- Gen.listOf(pathVariableGen)
  } yield RuleVariable(host, pathVariable)

  val validDefinitionsGen: Gen[ValidDefinitions] = for {
    nonEmptyAnnotations <- implicitly[Arbitrary[Map[NonEmptyString, NonEmptyString]]].arbitrary
    _ = println(nonEmptyAnnotations)
    annotations = nonEmptyAnnotations.map {case(key, value) => (key.value, value.value)}
    name <- arbitraryNonEmptyString.arbitrary.map(_.value)
    namespace <- arbitraryNonEmptyString.arbitrary.map(_.value)
    ruleVariables <- Gen.listOf(ruleVariableGen)
  } yield ValidDefinitions(annotations, name, namespace, ruleVariables)


}
