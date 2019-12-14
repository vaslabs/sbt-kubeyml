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

package kubeyml.ingress.plugin

import java.io.File

import cats.kernel.Semigroup
import kubeyml.ingress._
import kubeyml.service.Service
import cats.syntax.semigroup._
import kubeyml.deployment.Deployment
import json_support._
import kubeyml.plugin._
import sbt.AutoPlugin
import sbt.util.Logger


object KubeServicePlugin extends AutoPlugin {
  override def trigger = noTrigger
  override def requires = sbt.plugins.JvmPlugin

  override val projectSettings = sbt.inConfig(Keys.kube)(Keys.kubeymlSettings)

}

object Plugin {

  implicit val serviceValidationSemigroup: Semigroup[Either[List[IngressFailure], Ingress]] =
    (x: Either[List[IngressFailure], Ingress], y: Either[List[IngressFailure], Ingress]) => (x, y) match {
      case (Left(failures), Left(moreFailures)) => Left(failures ++ moreFailures)
      case (Right(_), l @ Left(_)) => l
      case (l @ Left(_), Right(_)) => l
      case (Right(_), r @ Right(_)) => r
    }

  sealed trait IngressFailure {
    def message: String
  }
  case class PortMappingFailure private[plugin] (port: Int) extends IngressFailure {
    override def message: String = s"Port $port declared in ingress was not found in the service definition"
  }
  case class ServiceNameFailure private[plugin] (serviceName: String, ingressService: String) extends IngressFailure {
    override def message: String =
      s"Service name '$ingressService' in ingress does not match name in service definition $serviceName"
  }

  private[plugin] def validatePortMappings(service: Service, ingress: Ingress): Either[List[IngressFailure], Ingress] = {
    ingress match {
      case CustomIngress(_, _, _, Spec(rules)) =>
        val validatedResult = rules.flatMap {
          case HttpRule(_, paths) =>
            paths.map {
              case Path(ServiceMapping(name, port), _) =>
                val portMatching =
                  service.spec.ports.find(_.port == port).map(_ => ingress)
                    .toRight[List[IngressFailure]](List(PortMappingFailure(port.value)))

                val nameMatching = Either.cond(
                  name == service.name,
                  ingress,
                  List(ServiceNameFailure(service.name.value, name.value))
                )
                portMatching |+| nameMatching
            }
        }.fold(Right(ingress))(_ |+| _)
        validatedResult
    }
  }

  def generate(deployment: Deployment, service: Service, ingress: Ingress, buildTarget: File, log: Logger) = {
    validatePortMappings(service, ingress).map {
      ingress =>
        kubeyml.service.plugin.Plugin.generate(deployment, service, buildTarget)
        generateIngress(ingress, buildTarget)
    }.left.map(_.foreach(f => log.error(f.message))).merge
  }

  private[plugin] def generateIngress(ingress: Ingress, buildTarget: File): Unit = {
    writePlan(ingress, buildTarget, "ingress")
  }


}
