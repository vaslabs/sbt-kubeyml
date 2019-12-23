package kubeyml.runbook

import java.time.LocalDateTime

import kubeyml.deployment.{Deployment, HttpGet, HttpProbe}
import kubeyml.ingress.{CustomIngress, HttpRule, Ingress}
import kubeyml.runbook.Runbook._
import kubeyml.runbook.Runbook.Properties._
case class Runbook(
  project: Project,
  contact: Contact,
  hostedIn: HostedIn,
  vcs: Option[Vcs],
  healthCheck: HealthCheck,
  `ci/cd`: Option[`Ci/Cd`],
  dashboards: Option[Dashboards],
  troubleshooting: Option[Troubleshooting],
  architecture: Option[Architecture]
)

object Runbook {

  def apply(
     deployment: Deployment,
     ingress: Ingress,
     additionalProperties: Set[Property] = Set.empty): Runbook = {

    val livenessProbe =
      deployment.spec.template.spec.containers.map(_.livenessProbe).flatMap {
        case HttpProbe(HttpGet(path, _, _), _, _, _, _, _) =>
          List(path)
        case _ => List.empty
      }.head.value

    val hostnames = ingress match {
      case CustomIngress(_, _, _, spec) =>
        spec.rules.map {
          case HttpRule(host, _) =>
            host.value
        }
    }

    val healthCheck = HealthCheck(livenessProbe)
    val endpoints = Hostname(hostnames)
    val namespace = Namespace(deployment.metadata.namespace.value)
    val project = Project(deployment, additionalProperties)
    val contact = Contact(additionalProperties)
    val vcs = Vcs.of(additionalProperties)
    val `ci\cd` = `Ci/Cd`.of(additionalProperties)
    val dashboards = Dashboards.of(additionalProperties)
    val troubleshooting = Troubleshooting.of(additionalProperties)
    val architecture = Architecture.of(additionalProperties)

    Runbook(
      project,
      contact,
      HostedIn(namespace, endpoints),
      vcs,
      healthCheck,
      `ci\cd`,
      dashboards,
      troubleshooting,
      architecture
    )
  }

  case class Project(projectTitle: ProjectTitle, projectDescription: Description)
  object Project {
    def apply(deployment: Deployment, additionalProperties: Set[Property]): Project = {
      val projectTitle = ProjectTitle(deployment.metadata.name.value)
      val projectDescription =
        additionalProperties.collectFirst {
          case d: Description => d
        }.getOrElse(
          Description("No description provided")
        )

      Project(projectTitle, projectDescription)
    }
  }

  case class Contact(
    lead: Option[Lead],
    developers: Option[Developers],
    testing: Option[Testing],
    supportTeam: Option[SupportTeam],
    supportHours: Option[SupportHours]
  )
  case class HostedIn(namespace: Namespace, endpoints: Hostname)

  object Contact {
    def apply(additionalProperties: Set[Property]): Contact = {
      val lead = additionalProperties.collectFirst {
        case lead: Lead => lead
      }
      val developer = additionalProperties.collectFirst {
        case d: Developers => d
      }
      val testing = additionalProperties.collectFirst {
        case t: Testing => t
      }
      val supportTeam = additionalProperties.collectFirst {
        case supportTeam: SupportTeam => supportTeam
      }
      val supportHours = additionalProperties.collectFirst {
        case supportHours: SupportHours => supportHours
      }

      Contact(lead, developer, testing, supportTeam, supportHours)

    }
  }

  sealed trait Property {
    val name: String
  }

  object Properties {

    case class ProjectTitle(value: String) extends Property {
      override val name: String = "PROJECT_TITLE"
    }

    case class Description(value: String) extends Property {
      override val name: String = Description.key
    }
    case object Description {
      val key = "PROJECT_DESCRIPTION"
    }
    case class Lead(value: String) extends Property {
      override val name: String = "LEAD"
    }

    case class Developers(value: String) extends Property {
      override val name: String = "DEVELOPERS"
    }

    case class Testing(testers: List[String]) extends Property {
      override val name: String = "TESTERS"
    }

    case class SupportTeam(teamName: String) extends Property {
      override val name: String = "SUPPORT_TEAM"
    }

    case class SupportHours(
           from: LocalDateTime,
           to: LocalDateTime, otherInfo: String) extends Property {
      override val name: String = "SUPPORT_HOURS"
    }

    case class Namespace(value: String) extends Property {
      override val name: String = "NAMESPACE"
    }
    case class Hostname(endpoints: List[String]) extends Property {
      override val name: String = "ENDPOINT"
    }

    case class Vcs(url: String) extends Property {
      override val name: String = Vcs.key
    }
    object Vcs {
      val key = "VCS_URL"
      def of(additionalProperties: Set[Property]): Option[Vcs] = {
        additionalProperties.collectFirst {
          case vcs: Vcs =>
            vcs
        }
      }
    }
    case class HealthCheck(url: String) extends Property {
      override val name: String = HealthCheck.key
    }
    case object HealthCheck {
      val key = "HEALTH_CHECK"
    }
    case class `Ci/Cd`(url: String) extends Property {
      override val name: String = `Ci/Cd`.key
    }

    object `Ci/Cd` {
      val key = "CI_CD_URL"
      def of(additionalProperties: Set[Property]): Option[`Ci/Cd`] =
        additionalProperties.collectFirst {
          case c: `Ci/Cd` => c
        }
    }

    case class Dashboards(dashboards: Dashboard, link: String) extends Property {
      override val name: String = Dashboards.key
    }

    object Dashboards {
      val key = "MONITORING_DASHBOARDS"
      def of(additionalProperties: Set[Property]): Option[Dashboards] = additionalProperties.collectFirst{
        case d: Dashboards => d
      }
    }

    case class Troubleshooting(knownIssues: List[(Problem, Solution)]) extends Property {
      override val name: String = Troubleshooting.key
    }

    object Troubleshooting {
      val key = "TROUBLESHOOTING"
      def of(additionalProperties: Set[Property]) = additionalProperties.collectFirst {
        case t: Troubleshooting => t
      }
    }

    case class Architecture(imageUrl: String, dependencies: List[Dependency]) extends Property {
      override val name: String = Architecture.key
    }

    object Architecture {
      val key = "ARCHITECTURE_DESCRIPTION"

      def of(additionalProperties: Set[Property]) = additionalProperties.collectFirst {
        case a: Architecture => a
      }
    }

  }

  case class Dashboard(name: String, description: String)
  case class Problem(title: String, description: String)
  case class Solution(description: String)
  case class Dependency(
       name: String,
       namespace: Option[String],
       isExternal: Boolean
  )

}
