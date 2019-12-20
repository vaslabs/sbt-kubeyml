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

package kubeyml.deployment.examples
import kubeyml.deployment._
import kubeyml.deployment.api._

import scala.concurrent.duration._
import io.circe.yaml.syntax._
import io.circe.syntax._
import kubeyml.ingress.{CustomIngress, Host, HttpRule, Ingress, ServiceMapping, Path => IngressPath, Spec => IngressSpec}
import kubeyml.ingress.api.Annotate
import kubeyml.service.Service
import kubeyml.ingress.json_support._

object NginxExample extends App {

  val deployment = deploy.namespace("yournamespace")
    .service("nginx-deployment")
    .withImage("nginx:1.7.9")
    .withProbes(
      livenessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty), period = 10 seconds),
      readinessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty), failureThreshold = 10)
    ).replicas(3)
    .pullDockerImage(IfNotPresent)
    .addPorts(List(Port(None, 80))
  )


  val service = Service.fromDeployment(deployment)

  val ingress: Ingress = CustomIngress(
    "nginx-ingress",
    "yournamespace",
    Map(Annotate.nginxRewriteTarget("/"), Annotate.nginxIngress()),
    IngressSpec(
      List(HttpRule(Host("your-host.domain.smth"),
      List(IngressPath(ServiceMapping("nginx-deployment", 80), "/testpath"))))
    )
  )

  println(ingress.asJson.asYaml.spaces4)


}
