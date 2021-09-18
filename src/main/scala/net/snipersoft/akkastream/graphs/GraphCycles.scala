package net.snipersoft.akkastream.graphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Source}

object GraphCycles extends App {
  /*
  if we have cycles we have risk of deadlocking:
  -limit number of elements in cycle (mergePreferredSolution/bufferSolution)
   */
  implicit val system: ActorSystem = ActorSystem("system")

  //  graphCycleDeadlock()
  //  mergePreferredSolution()
  bufferSolution()

  def graphCycleDeadlock(): Unit = {
    val accelerator = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sourceShape = builder.add(Source(1 to 100))
      val mergeShape = builder.add(Merge[Int](2))
      val incrementerShape = builder.add(Flow[Int].map { x => println(s"Accelerating $x"); x + 1 })

      sourceShape ~> mergeShape ~> incrementerShape
      mergeShape <~ incrementerShape

      ClosedShape
    }

    /*
    graph cycle deadlock
    we will see only first element - buffers will fast be full
     */
    RunnableGraph.fromGraph(accelerator).run()
  }

  def mergePreferredSolution(): Unit = {
    val accelerator = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sourceShape = builder.add(Source(1 to 100))
      val mergeShape = builder.add(MergePreferred[Int](1))
      val incrementerShape = builder.add(Flow[Int].map { x => println(s"Accelerating $x"); x + 1 })

      sourceShape ~> mergeShape ~> incrementerShape
      mergeShape.preferred <~ incrementerShape

      ClosedShape
    }

    RunnableGraph.fromGraph(accelerator).run()
  }

  def bufferSolution(): Unit = {
    val accelerator = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val sourceShape = builder.add(Source(1 to 100))
      val mergeShape = builder.add(Merge[Int](2))
      val repeaterShape = builder.add(
        Flow[Int].buffer(10, OverflowStrategy.dropHead)
          .map { x => println(s"Repeating $x"); Thread.sleep(100); x }
      ) //next elements will get from the source

      sourceShape ~> mergeShape ~> repeaterShape
      mergeShape <~ repeaterShape

      ClosedShape
    }

    RunnableGraph.fromGraph(accelerator).run()
  }
}
