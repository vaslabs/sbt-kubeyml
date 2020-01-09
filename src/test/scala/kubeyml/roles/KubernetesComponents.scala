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

import io.circe.Json
import kubeyml.protocol.NonEmptyString
import org.scalacheck.Gen
import io.circe.yaml.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import kubeyml.deployment.api._
import kubeyml.roles.ApiGroup.Core
trait KubernetesComponents {

  private val arbitraryNonEmptyString: Gen[NonEmptyString] =
    Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).map(NonEmptyString)

  case class ValidDefinition(metadata: MetadataDefinition, rules: List[RuleDefinition])
  case class MetadataDefinition(name: String, namespace: String)
  case class RuleDefinition(apiGroups: Set[String], resources: Set[String], verbs: Set[String])

  val ruleDefinition = Gen.listOf(for {
    apiGroups <- Gen.listOf(Gen.oneOf(Seq(""))).map(_.toSet)
    resources <- Gen.listOf(Gen.oneOf(Seq("pods"))).map(_.toSet)
    verbs <- Gen.listOf(Gen.oneOf("get", "watch", "list")).map(_.toSet)
  } yield RuleDefinition(apiGroups, resources, verbs))

  val validDefinition = for {
    name <- arbitraryNonEmptyString
    namespace <- arbitraryNonEmptyString
    rules <- ruleDefinition
  } yield ValidDefinition(MetadataDefinition(name.value, namespace.value), rules)


  def roleToJson(validDefinition: ValidDefinition): Json = parse(
    """
      |kind: Role
      |apiVersion: rbac.authorization.k8s.io/v1
      |""".stripMargin
  ).right.get deepMerge validDefinition.asJson


  def toRole(validDefinition: ValidDefinition): Role = {
    val rules = for {
      rule <- validDefinition.rules
      apiGroups = rule.apiGroups.map {
        case "" => Core
      }.toList
      resources = rule.resources.map {
        case "pods" => Pods
      }.toList
      verbs = rule.verbs.map {
        case "watch" => Verb.Watch
        case "list" => Verb.List
        case "get" => Verb.Get
      }.toList
    } yield Rule(apiGroups, resources, verbs)
    Role(
      RoleMetadata(validDefinition.metadata.name, validDefinition.metadata.namespace),
      rules
    )
  }
}
