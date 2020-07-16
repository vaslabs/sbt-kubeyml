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

import org.scalacheck._
import kubeyml.deployment.api._
import kubeyml.protocol.{Host, NonEmptyString}
import json_support._
import io.circe.syntax._
import KubernetesComponents._
import org.scalacheck.util.ConsoleReporter

class IngressJsonSpec extends Properties("ingress"){

  override def overrideParameters(p: Test.Parameters): Test.Parameters =
    Test.Parameters.default
      .withTestCallback(ConsoleReporter(2))

  propertyWithSeed("validdefinitions", Some("4MkUn89okrR9LXj33b5-LOdHtvDqBd1MRNRuKmlZ90E=")) = Prop.forAll(validDefinitionsGen){ valid: ValidDefinitions => {
      val expectedJson = ingress(valid).right.get
      val httpRules = valid.rules.map {
        case ruleVariable =>
          val paths = ruleVariable.paths.map(p => Path(ServiceMapping(p.serviceName, p.port), p.value))
          HttpRule(Host(ruleVariable.host), paths)
      }

      val nonEmptyAnnotations: Map[NonEmptyString, String] = valid.annotations.map {
        case (key, value) => NonEmptyString(key) -> value
      }
      val ingressDef: Ingress = CustomIngress(valid.name, valid.namespace, nonEmptyAnnotations, Spec(httpRules))
      val generatedJson = ingressDef.asJson
      if (generatedJson != expectedJson) {
        println(generatedJson)
        println(expectedJson)
      }

      generatedJson == expectedJson

    }
  }


}