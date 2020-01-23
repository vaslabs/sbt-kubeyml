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

import kubeyml.protocol.NonEmptyString

case class Role(metadata: RoleMetadata, rules: List[Rule])


case class RoleMetadata(name: NonEmptyString, namespace: NonEmptyString)
case class Rule(apiGroups: List[ApiGroup], resources: List[Resource], verbs: List[Verb])

sealed trait ApiGroup
case object ApiGroup {
  case object Core extends ApiGroup
}

sealed trait Verb
object Verb {

  case object Watch extends Verb

  case object Get extends Verb

  case object List extends Verb

}

sealed trait Resource
case object Pods extends Resource

case class RoleBinding(metadata: RoleBindingMetadata, subjects: List[Subject], roleRef: RoleRef)

case class RoleBindingMetadata(name: NonEmptyString, namespace: NonEmptyString)

sealed trait Subject

case class UserSubject(
          serviceAccount: NonEmptyString = NonEmptyString("default"),
          namespace: NonEmptyString) extends Subject


case class RoleRef(role: Role)