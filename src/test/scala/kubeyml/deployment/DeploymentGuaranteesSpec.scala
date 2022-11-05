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

import kubeyml.deployment.api._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DeploymentGuaranteesSpec extends AnyWordSpec with Matchers {

  "deployment" must {
    "not accept empty docker images" in {
      assertThrows[IllegalArgumentException](
        deploy.namespace("namespace").service("application").withImage("")
      )
    }
    "not accept empty namespace" in {
      assertThrows[IllegalArgumentException](
        deploy.namespace("").service("application").withImage("image")
      )
    }
    "not accept empty service" in {
      assertThrows[IllegalArgumentException](
        deploy.namespace("namespace").service("").withImage("image")
      )
    }
  }
}
