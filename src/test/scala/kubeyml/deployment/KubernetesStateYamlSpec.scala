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

import KubernetesComponents._

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.parser._
import kubeyml.deployment.api._
import kubeyml.deployment.json_support._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KubernetesStateYamlSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

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

  "kubernetes sample" must "have commands and args" in {

    forAll(lowEmptyChance) { deploymentTestParts: DeploymentTestParts =>
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
          .addCommand(Some("webserver"), Seq("/path/to/config"))
          .env(envName, envValue)

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
                                    |          command:
                                    |            - webserver
                                    |          args:
                                    |            - /path/to/config
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
                                    |            - name: "${envName}"
                                    |              value: "${envValue}"
                                    |""".stripMargin)
        val actualJson = Right(deployment.asJson)

        actualJson shouldBe expectedYaml
      }
    }
  }

  "kubernetes sample" must "have no commands apart from args" in {

    forAll(lowEmptyChance) { deploymentTestParts: DeploymentTestParts =>
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
          .addCommand(None, Seq("/path/to/config"))
          .env(envName, envValue)

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
                                    |          args:
                                    |            - /path/to/config
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
                                    |            - name: "${envName}"
                                    |              value: "${envValue}"
                                    |""".stripMargin)
        val actualJson = Right(deployment.asJson)

        actualJson shouldBe expectedYaml
      }
    }
  }

  "kubernetes sample" must "have no commands or args" in {

    forAll(lowEmptyChance) { deploymentTestParts: DeploymentTestParts =>
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
          .addCommand(None, Seq())
          .env(envName, envValue)

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
        val actualJson = Right(deployment.asJson)

        actualJson shouldBe expectedYaml
      }
    }
  }

  "kubernetes sample" must "be generated from definition" in {

    forAll(lowEmptyChance) { deploymentTestParts: DeploymentTestParts =>
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
        val actualJson = Right(deployment.asJson)

        actualJson shouldBe expectedYaml
      }
    }
  }

  "cpu and memory" must "be encoded as strings with m and MiB indicators" in {
    Cpu(500).asJson shouldBe "500m".asJson
    Memory(128).asJson shouldBe "128Mi".asJson

    val actualResourcesJson = Right(
      Resources(Resource(Cpu(500), Memory(128)), Resource(Cpu(1000), Memory(512))).asJson
    )
    val expectedResourcesJson = parse(
      """
          requests:
            memory: "128Mi"
            cpu: "500m"
          limits:
            memory: "512Mi"
            cpu: "1000m"
        """
    )

    actualResourcesJson shouldBe expectedResourcesJson
  }

}
