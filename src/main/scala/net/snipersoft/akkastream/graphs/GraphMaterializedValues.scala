package net.snipersoft.akkastream.graphs

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future

object GraphMaterializedValues extends App {
  implicit val system: ActorSystem = ActorSystem("system")

  import system.dispatcher

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm", "Hi"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((n, _) => n + 1)
  val collecter = Sink.fold[List[String], String](Nil)((acc, s) => s :: acc)

  //materializingSink()
  //materializingMultipleValues()
  counterFlow()

  def materializingSink(): Unit = {
    val complexWordSink = Sink.fromGraph(
      GraphDSL.createGraph(counter) { implicit builder =>
        counterShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[String](2))
          val onlyLowercase = builder.add(Flow[String].filter(s => s.toLowerCase == s))
          val onlyShort = builder.add(Flow[String].filter(_.length < 5))

          broadcast ~> onlyLowercase ~> printer
          broadcast ~> onlyShort ~> counterShape

          SinkShape(broadcast.in)
      }
    )

    //  val f: Future[Int] = wordSource.toMat(complexWordSink)(Keep.right).run()
    val f: Future[Int] = wordSource.runWith(complexWordSink) //shorter
    f.onComplete(println)
  }

  def materializingMultipleValues(): Unit = {
    case class AnalysisResult(lowerCaseTexts: List[String], shortText: Int)

    def prepareResult(lowerCaseFuture: Future[List[String]], shortFuture: Future[Int]) = for {
      lc <- lowerCaseFuture
      s <- shortFuture
    } yield AnalysisResult(lc, s)

    val complexWordSink = Sink.fromGraph(
      GraphDSL.createGraph(collecter, counter)
      ((collecterMatValue, counterMatValue) => prepareResult(collecterMatValue, counterMatValue)) { implicit builder =>
        (collecterShape, counterShape) =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[String](2))
          val onlyLowercase = builder.add(Flow[String].filter(s => s.toLowerCase == s))
          val onlyShort = builder.add(Flow[String].filter(_.length < 5))

          broadcast ~> onlyLowercase ~> collecterShape
          broadcast ~> onlyShort ~> counterShape

          SinkShape(broadcast.in)
      }
    )

    val f: Future[AnalysisResult] = wordSource.runWith(complexWordSink)
    f.onComplete(println)
  }

  def counterFlow(): Unit = {
    def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
      val counter = Sink.fold[Int, B](0)((counter, _) => counter + 1)

      val enhanceFlowStaticGraph = GraphDSL.createGraph(counter) { implicit builder =>
        counterShape =>
          import GraphDSL.Implicits._

          val originalFlowShape = builder.add(flow)
          val broadcast = builder.add(Broadcast[B](2))
          originalFlowShape ~> broadcast ~> counterShape

          FlowShape(originalFlowShape.in, broadcast.out(1))
      }

      Flow.fromGraph(enhanceFlowStaticGraph)
    }

    val originalFlow = Flow[String].map(_ + "!")
    val countingFlow: Flow[String, String, Future[Int]] = enhanceFlow(originalFlow)
    val f: Future[Int] = Source(1 to 10).map(_.toString)
      .viaMat(countingFlow)(Keep.right) //right - materialized value from counting flow (new element)
      .toMat(Sink.ignore)(Keep.left).run() //left - materialized value from counting flow  (previous element)
    f.onComplete(println)
  }
}
