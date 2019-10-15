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

  "image pull policy" must "decode to enumerateable string" in {
    val imagePullPolicyAlways: ImagePullPolicy = Always
    val imagePullPolicyIfNotPresent: ImagePullPolicy = IfNotPresent

    imagePullPolicyAlways.asJson shouldBe Json.fromString("Always")
    imagePullPolicyIfNotPresent.asJson shouldBe Json.fromString("IfNotPresent")
  }

  "kubernetes sample" must "be generated from definition" in {
    val expectedServiceName = Gen.alphaStr.sample.get
    val expectedNamespace = Gen.alphaStr.sample.get
    val expectedMetadataKey = Gen.alphaStr.sample.get
    val expectedMetadataValue = Gen.alphaNumStr.sample.get
    val expectedDockerImage = Gen.alphaNumStr.sample.get
    val deployment = Deployment(
      metadata = DeploymentMetadata(expectedServiceName, expectedNamespace),
      spec = Spec(
                replicas = 1,
                selector = Selector(MatchLabels(Some(expectedServiceName))),
                strategy = RollingUpdate(),
                template = Template(
                  metadata = TemplateMetadata(
                    Labels(expectedServiceName),
                    Map(expectedMetadataKey -> expectedMetadataValue)),
                  spec = TemplateSpec(
                    List(
                      Container(
                        image = expectedDockerImage,
                        imagePullPolicy = Always,
                        name = expectedServiceName,
                        ports = List(Port(Some("app"), 8080)),
                        livenessProbe = HttpProbe(HttpGet("/health", 8080, List.empty), 3 seconds, 5 seconds, None),
                        readinessProbe = NoProbe)
                      )
                    )
                  )
      )
    )

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
        |""".stripMargin).right.get

    deployment.asJson shouldBe expectedYaml
  }

}
