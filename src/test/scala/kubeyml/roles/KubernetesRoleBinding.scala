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

package kubeyml.roles
import io.circe.yaml.parser._
import kubeyml.protocol.NonEmptyString
import org.scalacheck.Gen
trait KubernetesRoleBinding {

  def binding(namespace: String, name: String, serviceAccount: String, roleName: String) = parse(
    s"""
      |kind: RoleBinding
      |apiVersion: rbac.authorization.k8s.io/v1
      |metadata:
      |  name: "$name"
      |  namespace: "$namespace"
      |subjects:
      |  - kind: User
      |    name: system:serviceaccount:$namespace:$serviceAccount
      |roleRef:
      |  kind: Role
      |  name: "$roleName"
      |  apiGroup: rbac.authorization.k8s.io
      |""".stripMargin
  )

  private val arbitraryNonEmptyString: Gen[NonEmptyString] =
    Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).map(NonEmptyString)

  val roleBindingGen: Gen[(String, RoleBinding)] = for {
    name <- arbitraryNonEmptyString
    namespace <- arbitraryNonEmptyString
    serviceAccount <- arbitraryNonEmptyString
    roleName <- arbitraryNonEmptyString
  } yield serviceAccount.value -> RoleBinding(
    RoleBindingMetadata(name, namespace),
    List(UserSubject(serviceAccount, namespace)),
    RoleRef(Role(RoleMetadata(roleName, namespace), List.empty))
  )

}
