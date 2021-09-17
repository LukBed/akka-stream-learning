package net.snipersoft.akkastream.graphs

import akka.actor.ActorSystem
import akka.stream.{FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

//open component - source, sink, flow
object OpenGraphs extends App {
  implicit val system: ActorSystem = ActorSystem("system")

  //  compositeSource()
  //  complexSink()
//  complexFlowExercise()
  flowFromSinkAndSource()

  def compositeSource(): Unit = {
    //all elements from the first source, then all elements from the second source

    val firstSource = Source(1 to 10)
    val secondSource = Source(24 to 1000)

    val sourceGraph = Source.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val concat = builder.add(Concat[Int](2))
        firstSource ~> concat
        secondSource ~> concat

        SourceShape(concat.out)
      }
    )

    sourceGraph.to(Sink.foreach(println)).run()
  }

  def complexSink(): Unit = {
    val firstSink = Sink.foreach[Int](el => println(s"A: $el"))
    val secondSink = Sink.foreach[Int](el => println(s"B: $el"))

    val sinkGraph = Sink.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))
        broadcast ~> firstSink
        broadcast ~> secondSink

        SinkShape(broadcast.in)
      }
    )

    Source(1 to 10).to(sinkGraph).run()
  }

  def complexFlowExercise(): Unit = {
    val firstFlow = Flow[Int].map(_ + 1)
    val secondFlow = Flow[Int].map(_ * 10)

    val flowGraph = Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        //operate on shapes, not components!
        val firstShape = builder.add(firstFlow)
        val secondShape = builder.add(secondFlow)
        firstShape ~> secondShape
        FlowShape(firstShape.in, secondShape.out)
      }
    )

    Source(1 to 10).via(flowGraph).to(Sink.foreach(println)).run()
  }

  def flowFromSinkAndSource(): Unit = {
    //very weird - flow from two independent components
    val sink = Sink.foreach[Int](println)
    val source = Source(1 to 10)

    val flowGraph = Flow.fromGraph(
      GraphDSL.create() { implicit builer =>
        val sinkShape = builer.add(sink)
        val sourceShape = builer.add(source)
        FlowShape(sinkShape.in, sourceShape.out)
      }
    )

    Source(1 to 2).via(flowGraph).to(Sink.foreach[Int](el => println(s"Final $el"))).run()

    /*
    Akka Stream API
    no connection between sink and source
    if sink is empty source doesn't know about it and can't finish the stream
     */
    Flow.fromSinkAndSource(sink, source)

    //with backpressure and termination signal
    Flow.fromSinkAndSourceCoupled(sink, source)
  }
}
