package net.snipersoft.akkastream.graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.duration.DurationInt

object GraphBasics extends App {
  implicit val system: ActorSystem = ActorSystem("system")

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)
  val output = Sink.foreach[(Int, Int)](println)

  val firstSink = Sink.foreach[Int](el => println(s"A $el"))
  val secondSink = Sink.foreach[Int](el => println(s"B $el"))

//  runnableGraphExample()
//  feedTwoSinksFromOneSource()
  balanceGraph()

  def runnableGraphExample(): Unit = {
    //shape => static graph => runnable graph => running graph
    val graph = RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => //mutable builder
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2)) //fan-out operator: 1 input, 2 outputs
        val zip = builder.add(Zip[Int, Int]) //fan-in: 1 input, 2 outputs

        input ~> broadcast //input feeds into broadcast, it mutates builder
        broadcast.out(0) ~> incrementer ~> zip.in0
        broadcast.out(1) ~> multiplier ~> zip.in1
        zip.out ~> output

        ClosedShape //freeze the builder shape, it becomes immutable
        //shape
      } //static graph
    ) //runnable graph

    graph.run()
  }

  def feedTwoSinksFromOneSource(): Unit = {
    val graph = RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))

//        input ~> broadcast.in
//        broadcast.out(0) ~> firstSink
//        broadcast.out(1) ~> secondSink

        input ~> broadcast ~> firstSink //shorter, implicit port numbering (not Scala implicits)
                 broadcast ~> secondSink
        ClosedShape
      }
    )

    graph.run()
  }

  def balanceGraph(): Unit = {
    val fastSource = input.throttle(5, 1.seconds)
    val slowSource = input.throttle(2, 1.seconds).map(_+1000)

    val graph = RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val merge = builder.add(Merge[Int](2))
        val balance = builder.add(Balance[Int](2))

        fastSource ~> merge ~> balance ~> firstSink
        slowSource ~> merge
        balance ~> secondSink

//        fastSource ~> merge.in(0)
//        slowSource ~> merge.in(1)
//        merge ~> balance
//        balance.out(0) ~> firstSink
//        balance.out(1) ~> secondSink

        ClosedShape
      }
    )

    graph.run()
  }
}
