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
import io.circe.yaml.parser.parse
import kubeyml.protocol.NonEmptyString
import org.scalacheck.Gen
import io.circe.syntax._
import cats.implicits._


case class PathVariable(serviceName: String, port: Int, value: String)

case class RuleVariable(host: String, paths: List[PathVariable])

case class ValidDefinitions(
       annotations: Map[String, String],
       name: String,
       namespace: String,
       rules: List[RuleVariable]
)



trait KubernetesComponents {


  private val arbitraryNonEmptyString: Gen[NonEmptyString] =
    Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).map(NonEmptyString)

  private val pathVariableGen: Gen[PathVariable] = for {
    serviceName <- arbitraryNonEmptyString.map(_.value)
    servicePort <- Gen.chooseNum(0, 65535)
    path <- Gen.oneOf(arbitraryNonEmptyString.map(s => s"/$s"), Gen.const("/"))
  } yield PathVariable(serviceName, servicePort, path)

  private val ruleVariableGen: Gen[RuleVariable] = for {
    host <- Gen.oneOf(
      Gen.listOfN(2, arbitraryNonEmptyString.map(_.value)).map(_.mkString(".")),
      Gen.listOfN(3, arbitraryNonEmptyString.map(_.value)).map(_.mkString(".")),
      Gen.listOfN(4, arbitraryNonEmptyString.map(_.value)).map(_.mkString(".")),
      Gen.const("localhost")
    )
    pathVariable <- Gen.listOf(pathVariableGen)
  } yield RuleVariable(host, pathVariable)

  private val annotationsGen = Gen.mapOf (for {
      key <- arbitraryNonEmptyString
      value <- Gen.alphaNumStr
    } yield key.value -> value
  )

  val validDefinitionsGen: Gen[ValidDefinitions] = for {
    name <- arbitraryNonEmptyString.map(_.value)
    namespace <- arbitraryNonEmptyString.map(_.value)
    ruleVariables <- Gen.listOf(ruleVariableGen)
    annotations <- annotationsGen
  } yield ValidDefinitions(annotations, name, namespace, ruleVariables)


}

object KubernetesComponents extends KubernetesComponents {

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
}