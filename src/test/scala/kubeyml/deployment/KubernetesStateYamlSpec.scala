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
import org.scalatest.{FlatSpec, Matchers}
import io.circe.yaml.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import json_support._
import org.scalacheck.Gen

import scala.concurrent.duration._

class KubernetesStateYamlSpec extends FlatSpec with Matchers{

  "deployment strategy" must "have the correct schema" in {
    val expectedStrategy = parse(
      """
        |type: RollingUpdate
        |rollingUpdate:
        |   maxSurge: 0
        |   maxUnavailable: 1
        |""".stripMargin
    ).right.get
    RollingUpdate().asJson shouldBe expectedStrategy

    val deploymentStrategy: DeploymentStrategy = RollingUpdate()
    deploymentStrategy.asJson shouldBe expectedStrategy
  }

  "image pull policy" must "decode to enumerable string" in {
    val imagePullPolicyAlways: ImagePullPolicy = Always
    val imagePullPolicyIfNotPresent: ImagePullPolicy = IfNotPresent

    imagePullPolicyAlways.asJson shouldBe Json.fromString("Always")
    imagePullPolicyIfNotPresent.asJson shouldBe Json.fromString("IfNotPresent")
  }

  "kubernetes sample" must "be generated from definition" in {
    import api._
    val expectedServiceName = Gen.alphaStr.sample.get
    val expectedNamespace = Gen.alphaStr.sample.get
    val expectedMetadataKey = Gen.alphaStr.sample.get
    val expectedMetadataValue = Gen.alphaNumStr.sample.get
    val expectedDockerImage = Gen.alphaNumStr.sample.get
    val expectedEnvVarName = Gen.alphaUpperStr.sample.get
    val expectedEnvVarValue = Gen.alphaStr.sample.get
    val deployment = deploy.namespace(expectedNamespace)
      .service(expectedServiceName)
      .withImage(expectedDockerImage)
      .withProbes(
        livenessProbe = HttpProbe(HttpGet("/health", 8080, List.empty), initialDelay = 3 seconds, period = 5 seconds),
        readinessProbe = NoProbe
      )
      .replicas(1)
      .addPorts(List(Port(Some("app"), 8080)))
      .annotateSpecTemplate(Map(expectedMetadataKey -> expectedMetadataValue))
      .env(expectedEnvVarName, expectedEnvVarValue)

    val expectedYaml = parse(
      s"""
        |apiVersion: apps/v1
        |kind: Deployment
        |metadata:
        |  name: &name ${expectedServiceName}
        |  namespace: ${expectedNamespace}
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
        |        ${expectedMetadataKey}: ${expectedMetadataValue}
        |    spec:
        |      containers:
        |        - image: ${expectedDockerImage}
        |          imagePullPolicy: Always
        |          name: *name
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
        |            - name: ${expectedEnvVarName}
        |              value: ${expectedEnvVarValue}
        |""".stripMargin).right.get

    deployment.asJson shouldBe expectedYaml
  }

  "environment variables" must "decode to list of key values" in {
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
