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

import kubeyml.protocol.{NonEmptyString, PortNumber}

package object api {

  def deploy: EmptyDeployment =
    EmptyDeployment

  implicit def toNonEmptyString(value: String): NonEmptyString = {
    require(!value.isEmpty)
    NonEmptyString(value)
  }

  implicit def fromNonEmptyString(value: NonEmptyString): String = value.value

  implicit def toPortNumber(value: Int): PortNumber = PortNumber(value)
  implicit def fromPortNumber(portNumber: PortNumber): Int = portNumber.value

  implicit final class KubernetesDeploymentOps(val kubernetesDeployment: EmptyDeployment) extends AnyVal {
    def namespace(name: String): NamespaceDeployment = NamespaceDeployment(name)
  }

  implicit final class NamespaceDeploymentOps(val namespaceDeployment: NamespaceDeployment) extends AnyVal {
    def service(name: String): AppDeployment = AppDeployment(namespaceDeployment.namespace, name)
  }

  implicit final class AppDeploymentOps(val appDeployment: AppDeployment) extends AnyVal {

    def withImage(dockerImage: String): DockerisedAppDeployment =
      DockerisedAppDeployment(appDeployment.namespace, appDeployment.service, dockerImage)
  }

  implicit final class DockerisedAppDeploymentOps(val dockerisedAppDeployment: DockerisedAppDeployment) extends AnyVal {
    import dockerisedAppDeployment._

    def withProbes(livenessProbe: Probe, readinessProbe: Probe): Deployment = Deployment(
      metadata = DeploymentMetadata(name = dockerisedAppDeployment.service, namespace = namespace),
      spec = Spec(
        replicas = 2,
        selector = Selector(MatchLabels(Some(service))),
        template = Template(
          TemplateMetadata(
            Labels(service),
            Map.empty
          ),
          TemplateSpec(
            List(
              Container(
                service,
                image,
                None,
                Seq.empty,
                List.empty,
                IfNotPresent,
                livenessProbe,
                readinessProbe,
                Map.empty
              )
            )
          )
        ),
        strategy = RollingUpdate()
      )
    )
  }

  implicit final class DeploymentOps(val deployment: Deployment) extends AnyVal {

    def addPorts(ports: List[Port]): Deployment = deployment.addContainerPorts(ports)

    def replicas(number: Int): Deployment = deployment.copy(spec = deployment.spec.copy(replicas = number))

    def annotateTemplate(annotations: Map[String, String]) = deployment.annotateSpecTemplate(annotations)

    def envFromSecret(variableName: String, name: String, key: String): Deployment =
      deployment.addEnv(Map(EnvName(variableName) -> EnvSecretValue(name, key)))

    def envFromPath(variableName: String, fieldPath: String): Deployment =
      deployment.addEnv(Map(EnvName(variableName) -> EnvFieldValue(fieldPath)))

    def env(variableName: String, variableValue: String): Deployment =
      deployment.addEnv(Map(EnvName(variableName) -> EnvRawValue(variableValue)))

    def envs(envs: Map[EnvName, EnvValue]): Deployment =
      deployment.addEnv(envs)

    def requestResource(resource: Resource): Deployment =
      deployment.request(resource)

    def limitResource(resource: Resource): Deployment =
      deployment.limit(resource)

    def pullDockerImage(pullPolicy: ImagePullPolicy): Deployment =
      deployment.pullPolicy(pullPolicy)

    def rollingUpdate(rollingUpdate: RollingUpdate): Deployment =
      deployment.withUpdateStrategy(rollingUpdate)

    def recreate: Deployment =
      deployment.recreate
  }
}
