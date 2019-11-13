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

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.ScalacheckShapeless._

trait KubernetesComponents {

  private def strOrEmpty: Gen[String] = Gen.oneOf(Gen.const(""), Gen.alphaNumStr)

  def nonEmptyParts(deploymentTestParts: DeploymentTestParts) = {
    import deploymentTestParts._
    Seq(namespace, dockerImage, serviceName, envName, metadataKey, metadataValue).forall(_.nonEmpty)
  }

  val highEmptyChance: Gen[DeploymentTestParts] =
    for {
      serviceName <- strOrEmpty
      namespace <- strOrEmpty
      metaKey <- strOrEmpty
      metaValue <- strOrEmpty
      dockerImage <- strOrEmpty
      envName <- strOrEmpty
      envValue <- strOrEmpty
      deploymentTestParts = DeploymentTestParts(serviceName, namespace, metaKey, metaValue, dockerImage, envName, envValue)
      if !nonEmptyParts(deploymentTestParts)
    } yield deploymentTestParts

  val lowEmptyChance: Gen[DeploymentTestParts] = for {
      serviceName <- Gen.alphaNumStr
      namespace <- Gen.alphaNumStr
      metaKey <- Gen.alphaNumStr
      metaValue <- Gen.alphaNumStr
      dockerImage <- Gen.alphaNumStr
      envName <- Gen.alphaNumStr
      envValue <- Gen.alphaNumStr
    } yield DeploymentTestParts(serviceName, namespace, metaKey, metaValue, dockerImage, envName, envValue)


  implicit val nonEmptyStringGen: Arbitrary[NonEmptyString] =
    Arbitrary(Gen.alphaStr.filterNot(_.isEmpty).map(NonEmptyString))

  val environmentVariableTestPartsGen: Gen[EnvironmentVariableTestParts] = {
    implicit val arbitraryString: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)
    implicitly[Arbitrary[EnvironmentVariableTestParts]].arbitrary
  }.filterNot {
    case EnvironmentVariableTestParts(fieldPathName, _, secretEnvName, _, _, rawName, _) =>
      Seq(fieldPathName == secretEnvName, fieldPathName == rawName, secretEnvName == rawName)
        .fold(false)(_ || _)
  }

}

object KubernetesComponents extends KubernetesComponents

case class DeploymentTestParts(
      serviceName: String,
      namespace: String,
      metadataKey: String,
      metadataValue: String,
      dockerImage: String,
      envName: String,
      envValue: String
)

case class EnvironmentVariableTestParts(
   fieldPathName: NonEmptyString,
   fieldPathValue: NonEmptyString,
   secretEnvName: NonEmptyString,
   secretKey: NonEmptyString,
   secretName: NonEmptyString,
   rawName: NonEmptyString,
   rawValue: String
)