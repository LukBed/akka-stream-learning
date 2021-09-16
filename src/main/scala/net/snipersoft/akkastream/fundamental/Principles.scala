package net.snipersoft.akkastream.fundamental

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object Principles extends App {
  implicit val system: ActorSystem = ActorSystem("mySystem")

  //  basics()
  exercise()

  def basics() = {
    val source = Source(1 to 10) //publisher
    val sink = Sink.foreach[Int](println) //subscriber
    val graph = source.to(sink)
    graph.run()

    val flow = Flow[Int].map(_ + 1) //processor
    val sourceWithFlow = source.via(flow)
    val sinkWithFlow = flow.to(sink)

    sourceWithFlow.to(sinkWithFlow).run()

    //sources can emmit immutable and serializable objects (much like actor messages), but not nulls
    //  Source.single[String](null).to(Sink.foreach(println)) //NPE
    Source.single[Option[String]](None).to(Sink.foreach(println)).run() //OK
  }

  def sources() = {
    val oneElementSource = Source.single(1)
    val collectionSource = Source(List(1, 2, 3))
    val emptySource = Source.empty[Int]
    val infiniteSource = Source(LazyList.from(1))
    val futureSource = Source.future(Future(1))
  }

  def sinks() = {
    val boringSink = Sink.ignore
    val foreachSink = Sink.foreach[String](println)
    val headSink = Sink.head[Int] //retrieves head and then close the stream
    val foldSink = Sink.fold[Int, Int](0)(_ + _)
  }

  def flows() = {
    val mapFlow = Flow[Int].map(_ + 1)
    val takeFlow = Flow[Int].take(5) //finite stream
    val dropFlow = Flow[Int].drop(3)
    val filterFlow = Flow[Int].filter(_ % 2 == 0)
    //flatMap doesn't exist!
  }

  //val myGraph = source.via(mapFlow).via(takeFlow).to(sink)

  def syntacticSugar() = {
    Source(1 to 10).map(_ + 1).runForeach(println)
    Source(1 to 10).via(Flow[Int].map(_ + 1)).run().foreach(println)
  }

  def exercise() = {
    val names = List("Marysia", "Jan", "Ola", "Janek", "Marysieńka", "Roman")
    val source = Source(names).filter(_.length > 5).take(2)
    val filterFlow = Flow[String].filter(_.length > 5)
    val takeFlow = Flow[String].take(2)
    val sink = Sink.foreach[String](println)
    source.via(filterFlow).via(takeFlow).to(sink).run()
    Source(names).filter(_.length > 5).take(2).runForeach(println)
  }
}
