package kubeyml.ingress

import kubeyml.protocol.NonEmptyString

package object api {

  type AnnotationElement = (NonEmptyString, String)

  object Annotate {
    def nginxIngress(): AnnotationElement =
      NonEmptyString("kubernetes.io/ingress.class") -> "nginx"

    def nginxRewriteTarget(path: String): AnnotationElement =
      NonEmptyString("nginx.ingress.kubernetes.io/rewrite-target") -> "/update-settings"
  }
}
