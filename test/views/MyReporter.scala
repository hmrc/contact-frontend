package views

import org.scalatest.Reporter
import org.scalatest.events.{Event, SuiteCompleted, TestPending}

import scala.collection.mutable.ListBuffer

class MyReporter extends Reporter {
  var infos: ListBuffer[String] = new ListBuffer[String]()

  override def apply(event: Event): Unit = event match {
    case event:TestPending =>
      val className = event.testName.replace(s"should ${event.testText}", "")
        .trim
        .split("\\.")
        .last
      val classInstance = className.head.toLower + className.tail
      val caseCode = s"case $classInstance: $className => render($classInstance)"
      println("Missing wiring - add the following to your renderViewByClass function:\n" +
        s"\t$caseCode")

      infos += caseCode
    case _:SuiteCompleted =>
      println(s"\nAll wiring for renderViewByClass below:\n")
      println("override def renderViewByClass: PartialFunction[Any, Html] = {")
      infos.toList.foreach(c => println("\t" + c))
      println("}")
    case _ =>
  }
}
