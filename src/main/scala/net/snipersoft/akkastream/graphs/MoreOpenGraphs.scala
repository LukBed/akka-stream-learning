package net.snipersoft.akkastream.graphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, FanOutShape, FanOutShape2, Graph, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import java.util.Date

object MoreOpenGraphs extends App {
  implicit val system: ActorSystem = ActorSystem()

//  max3Operator()
  bankTransactions()

  //UniformFanInShape (the same types of all inputs and outputs)
  def max3Operator(): Unit = {
    val max3staticGraph = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
      val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

      max1.out ~> max2.in0

      UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
    }

    val firstSource = Source(1 to 10)
    val secondSource = Source(1 to 10).map(_ => 5)
    val thirdSource = Source((1 to 10).reverse)

    val maxSink = Sink.foreach[Int](el => println(s"Max. is $el"))

    val max3RunnableGraph = RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val max3 = builder.add(max3staticGraph)

        firstSource ~> max3
        secondSource ~> max3
        thirdSource ~> max3
        max3 ~> maxSink

        ClosedShape
      }
    )

    max3RunnableGraph.run()
  }

  //non-uniform fan out
  def bankTransactions(): Unit = {
    /*
    transaction =>
    => all transactions
    => suspicious transaction ids
    */

    case class Transaction(id: String, source: String, recipient: String, amount: Int, timestamp: Date)

    val transactionSource = Source(List(
      Transaction("id1", "Marysia", "Łukasz", 1200, new Date),
      Transaction("id2", "Janek", "Paweł", 800, new Date),
      Transaction("id3", "Kasia", "Ola", 1500, new Date),
      Transaction("id4", "Kurt", "Hans", 400, new Date),
    ))

    val bankProcessor = Sink.foreach[Transaction](println)

    val suspiciousAnalysisService = Sink.foreach[String](id => println(s"Transaction suspect: $id"))

    val suspiciousTransactionStaticGraph = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Transaction](2))
      val suspiciousFilter = builder.add(Flow[Transaction].filter(_.amount >= 1000))
      val idExtractor = builder.add(Flow[Transaction].map[String](_.id))

      broadcast.out(0) ~> suspiciousFilter ~> idExtractor

      new FanOutShape2(broadcast.in, broadcast.out(1), idExtractor.out)
    }

    val transactionRunnableGraph = RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val suspiciousTransactionShape = builder.add(suspiciousTransactionStaticGraph)

        transactionSource ~> suspiciousTransactionShape.in
        suspiciousTransactionShape.out0 ~> bankProcessor
        suspiciousTransactionShape.out1 ~> suspiciousAnalysisService

        ClosedShape
      }
    )

    transactionRunnableGraph.run()
  }

}
