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

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.parser._
import kubeyml.deployment.api._
import kubeyml.deployment.json_support._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._

class KubernetesStateYamlSpec extends FlatSpec with Matchers with ScalaCheckPropertyChecks {
  import KubernetesStateYamlSpec._

  "deployment strategy" must "have the correct schema" in {
    forAll { (maxSurge: Int, maxUnavailable: Int) =>
      whenever(maxSurge >= 0 && maxUnavailable >= 0) {
        val expectedStrategy = parse(
          s"""
               |type: RollingUpdate
               |rollingUpdate:
               |   maxSurge: ${maxSurge}
               |   maxUnavailable: ${maxUnavailable}
               |""".stripMargin
        )
        Right(RollingUpdate(maxSurge, maxUnavailable).asJson) shouldBe expectedStrategy
      }
    }
  }

  "image pull policy" must "decode to enumerable string" in {
    val imagePullPolicyAlways: ImagePullPolicy = Always
    val imagePullPolicyIfNotPresent: ImagePullPolicy = IfNotPresent

    imagePullPolicyAlways.asJson shouldBe Json.fromString("Always")
    imagePullPolicyIfNotPresent.asJson shouldBe Json.fromString("IfNotPresent")
  }

  "kubernetes sample" must "be generated from definition" in {
    implicit val arbTest = Properties.lowEmptyChance
    forAll { deploymentTestParts: DeploymentTestParts =>
      import deploymentTestParts._
      whenever(nonEmptyParts(deploymentTestParts)) {
        val deployment = deploy
          .namespace(namespace)
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
          .addCommand(Seq("java"))

        val expectedYaml = parse(s"""
               |apiVersion: apps/v1
               |kind: Deployment
               |metadata:
               |  name: &name "${serviceName}"
               |  namespace: "${namespace}"
               |spec:
               |  replicas: 1
               |  selector:
               |    matchLabels:
               |      app: *name
               |  strategy:
               |    type: RollingUpdate
               |    rollingUpdate:
               |      maxSurge: 0
               |      maxUnavailable: 1
               |  template:
               |    metadata:
               |      labels:
               |        app: *name
               |      annotations:
               |        ${metadataKey}: "${metadataValue}"
               |    spec:
               |      containers:
               |        - image: "${dockerImage}"
               |          imagePullPolicy: IfNotPresent
               |          name: *name
               |          command:
               |            - java
               |          ports:
               |            - name: app
               |              containerPort: 8080
               |          livenessProbe:
               |            httpGet:
               |              path: /health
               |              port: 8080
               |            initialDelaySeconds: 3
               |            periodSeconds : 5
               |            successThreshold: 1
               |            failureThreshold: 3
               |            timeoutSeconds: 1
               |          resources:
               |            requests:
               |              memory: "256Mi"
               |              cpu: "500m"
               |            limits:
               |              memory: "512Mi"
               |              cpu: "1000m"
               |          env:
               |            - name: "${envName}"
               |              value: "${envValue}"
               |""".stripMargin)

        val someYaml = expectedYaml match {
          case Left(err) =>
            sys.error(err.message)
            None
          case Right(yaml) if yaml != deployment.asJson =>
            println(yaml.noSpaces)
            println(deployment.asJson.noSpaces)
            deployment.asJson shouldBe yaml
            Some(yaml)
          case Right(yaml) => Some(yaml)
        }
        assert(someYaml.nonEmpty)

      }
    }
  }

  "kubernetes deployment" must "report an error" in {
    implicit val arbTest = Properties.highEmptyChance

    forAll { deploymentParts: DeploymentTestParts =>
      whenever(!nonEmptyParts(deploymentParts)) {
        import deploymentParts._
        assertThrows[IllegalArgumentException](
          deploy
            .namespace(namespace)
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
        )
      }
    }
  }

  "environment variables" must "decode to list of key values" in {
    implicit val arbTest = Properties.highEmptyChance

    val expectedFieldPathName = Gen.alphaUpperStr.sample.get
    val expectedFieldPathValue = Gen.alphaLowerStr.sample.get

    val expectedSecretEnvName = Gen.alphaUpperStr.sample.get
    val expectedSecretKey = Gen.alphaLowerStr.sample.get
    val expectedSecretName = Gen.alphaLowerStr.sample.get

    val expectedRawName = Gen.alphaUpperStr.sample.get
    val expectedRawValue = Gen.alphaNumStr.sample.get

    val envYaml = parse(
      s"""
          - name: ${expectedFieldPathName}
            valueFrom:
              fieldRef:
                fieldPath: ${expectedFieldPathValue}
          - name: ${expectedSecretEnvName}
            valueFrom:
              secretKeyRef:
                key: ${expectedSecretKey}
                name: ${expectedSecretName}
          - name: ${expectedRawName}
            value: ${expectedRawValue}
      """
    ).right.get

    Map[EnvName, EnvValue](
      EnvName(expectedFieldPathName) -> EnvFieldValue(expectedFieldPathValue),
      EnvName(expectedSecretEnvName) -> EnvSecretValue(expectedSecretName, expectedSecretKey),
      EnvName(expectedRawName) -> EnvRawValue(expectedRawValue)
    ).asJson shouldBe envYaml
  }

  "cpu and memory" must "be encoded as strings with m and MiB indicators" in {
    Cpu(500).asJson shouldBe "500m".asJson
    Memory(128).asJson shouldBe "128Mi".asJson
    Resources(Resource(Cpu(500), Memory(128)), Resource(Cpu(1000), Memory(512))).asJson shouldBe parse(
      """
          requests:
            memory: "128Mi"
            cpu: "500m"
          limits:
            memory: "512Mi"
            cpu: "1000m"
        """
    ).right.get
  }

}

object KubernetesStateYamlSpec {
  case class DeploymentTestParts(serviceName: String,
                                 namespace: String,
                                 metadataKey: String,
                                 metadataValue: String,
                                 dockerImage: String,
                                 envName: String,
                                 envValue: String)

  private def strOrEmpty: Gen[String] =
    Gen.oneOf(Gen.const(""), Gen.alphaNumStr)

  def nonEmptyParts(deploymentTestParts: DeploymentTestParts) = {
    import deploymentTestParts._
    Seq(namespace, dockerImage, serviceName, envName, metadataKey, metadataValue).forall(_.nonEmpty)
  }

  object Properties {

    val highEmptyChance: Arbitrary[DeploymentTestParts] = Arbitrary(
      for {
        serviceName <- strOrEmpty
        namespace <- strOrEmpty
        metaKey <- strOrEmpty
        metaValue <- strOrEmpty
        dockerImage <- strOrEmpty
        envName <- strOrEmpty
        envValue <- strOrEmpty
      } yield DeploymentTestParts(serviceName, namespace, metaKey, metaValue, dockerImage, envName, envValue)
    )

    val lowEmptyChance: Arbitrary[DeploymentTestParts] = Arbitrary(
      for {
        serviceName <- Gen.alphaNumStr
        namespace <- Gen.alphaNumStr
        metaKey <- Gen.alphaNumStr
        metaValue <- Gen.alphaNumStr
        dockerImage <- Gen.alphaNumStr
        envName <- Gen.alphaNumStr
        envValue <- Gen.alphaNumStr
      } yield DeploymentTestParts(serviceName, namespace, metaKey, metaValue, dockerImage, envName, envValue)
    )

  }
}
