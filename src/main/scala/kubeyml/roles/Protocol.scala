package kubeyml.roles

import kubeyml.protocol.NonEmptyString

case class Role(metadata: RoleMetadata, rules: Rules)


case class RoleMetadata(name: NonEmptyString, namespace: NonEmptyString)
case class Rules(apiGroups: List[String], resources: List[Resource], verbs: List[Verb])

sealed trait Verb
case object Watch extends Verb
case object Get extends Verb
case object List extends Verb

sealed trait Resource
case object Pods extends Resource

case class RoleBinding(metadata: RoleBindingMetadata, subjects: List[Subject], roleRef: RoleRef)

case class RoleBindingMetadata(name: NonEmptyString, namespace: NonEmptyString)

sealed trait Subject

case class UserSubject(
          serviceAccount: NonEmptyString = NonEmptyString("default"),
          namespace: NonEmptyString) extends Subject


case class RoleRef(role: Role)