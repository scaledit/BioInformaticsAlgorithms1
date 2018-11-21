package akka.http.specs2

import akka.http.scaladsl.testkit.TestFrameworkInterface
import org.specs2.execute.{Failure, FailureException}

// until akka-http gets support
trait Specs2Interface extends TestFrameworkInterface {

  def failTest(msg: String): Nothing = {
    val trace = new Exception().getStackTrace.toList

    val ignoreList = List("org.specs2", "scala.collection", "java.util.")

    val fixedTrace = trace.filterNot(x => ignoreList.exists(x.getClassName.startsWith))
    throw new FailureException(Failure(msg, stackTrace = fixedTrace))
  }

}


