package net.snipersoft.akkastream.fundamental

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future

object Materializing extends App {
  implicit val system: ActorSystem = ActorSystem("mySystem")
  //  implicit val materializer = ActorMaterializer()

  //import scala.concurrent.ExecutionContext.Implicits.global

  import system.dispatcher //ec

  val source = Source(1 to 10)
  val flow = Flow[Int].map(_ + 1)
  val sink = Sink.foreach[Int](println)

  //  futureValue()
  //  chooseMaterializedValue()
  //  syntacticSugar()
  //  lastElementExercise()
  countWordsExercise()

  def futureValue() = {
    val sumSink: Sink[Int, Future[Int]] = Sink.reduce[Int](_ + _)
    val sumFuture: Future[Int] = source.runWith(sumSink)
    sumFuture.onComplete(println)
  }

  def chooseMaterializedValue() = {
    source.viaMat(flow)((sourceMat, flowMat) => flowMat) //sourceMat, flowMat - materialized values
    source.viaMat(flow)(Keep.left)
    source.viaMat(flow)(Keep.right)
    source.viaMat(flow)(Keep.both)
    source.viaMat(flow)(Keep.none) //NotUsed

    val graph: RunnableGraph[Future[Done]] = source.viaMat(flow)(Keep.right).toMat(sink)(Keep.right)
    val runGraph = graph.run()
    runGraph.onComplete(println)
  }

  def syntacticSugar() = {
    source.runWith(Sink.reduce[Int](_ + _))
    source.to(Sink.reduce(Keep.right)) //same
    source.runReduce(_ + _) //same also

    sink.runWith(source)
    source.to(sink).run() //left and right value changed

    flow.runWith(source, sink)
  }

  def lastElementExercise(): Unit = {
    source.runWith(Sink.reduce[Int]((_, v) => v)).onComplete(println)
    source.runWith(Sink.reduce(Keep.right)).onComplete(println)
    source.runWith(Sink.last).onComplete(println)
    source.runReduce(Keep.right).onComplete(println)
    source.toMat(Sink.last)(Keep.right).run().onComplete(println)
  }

  def countWordsExercise(): Unit = {
    def count(s: String): Int = s.split(' ').length

    val wordsSource = Source(List("Akka is awesome", "I love streams", "Materialized values are the best", "Hello my friend"))

    wordsSource.map(count).runWith(Sink.reduce[Int](_ + _)).onComplete(println)

    val foldSink = Sink.fold[Int, String](0)((acc, s) => acc + count(s))
    wordsSource.toMat(foldSink)(Keep.right).run().onComplete(println) //Future[Int]

    wordsSource.runFold[Int](0)((acc, s) => acc + count(s)).onComplete(println)
  }
}
