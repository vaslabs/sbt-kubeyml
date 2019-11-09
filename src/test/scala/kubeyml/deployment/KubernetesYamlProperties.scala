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

package kubeyml.deployment

import io.circe.yaml.parser.parse
import kubeyml.deployment.KubernetesComponents._
import kubeyml.deployment.api._
import org.scalacheck.Properties
import org.scalacheck._
import io.circe.Json
import io.circe.syntax._
import io.circe.yaml.parser._
import io.circe.generic.auto._
import kubeyml.deployment.api._
import kubeyml.deployment.json_support._
import scala.util.{Failure, Try}
import scala.concurrent.duration._

class KubernetesYamlProperties extends Properties("yaml"){


  property("yieldserror") = Prop.forAll(highEmptyChance) {
    deploymentParts: DeploymentTestParts => {
        import deploymentParts._
        Try(
          deploy.namespace(namespace)
            .service(serviceName)
            .withImage(dockerImage)
            .withProbes(
              livenessProbe = HttpProbe(HttpGet("/health", 8080, List.empty), initialDelay = 3 seconds, period = 5 seconds),
              readinessProbe = NoProbe
            )
            .replicas(1)
            .addPorts(List(Port(Some("app"), 8080)))
            .annotateSpecTemplate(Map(metadataKey -> metadataValue))
            .env(envName, envValue)
        ) match {
          case Failure(_: IllegalArgumentException) => true
          case _ => false
        }
    }
  }

  property("environmentvars") = Prop.forAll(environmentVariableTestPartsGen) { environmentVariableTestParts =>
    import environmentVariableTestParts._
    val envYaml = parse(
      s"""
            - name: ${fieldPathName.value}
              valueFrom:
                fieldRef:
                  fieldPath: ${fieldPathValue.value}
            - name: ${secretEnvName.value}
              valueFrom:
                secretKeyRef:
                  key: ${secretKey.value}
                  name: ${secretName.value}
            - name: ${rawName.value}
              value: "${rawValue}"
        """
    )
    val actualJson = Map[EnvName, EnvValue](
      EnvName(fieldPathName) -> EnvFieldValue(fieldPathValue),
      EnvName(secretEnvName) -> EnvSecretValue(secretName, secretKey),
      EnvName(rawName) -> EnvRawValue(rawValue)
    ).asJson

    Right(actualJson) == envYaml

  }
}
