package net.snipersoft.akkastream

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {
  implicit val actorSystem: ActorSystem = ActorSystem("system")

  val source = Source(1 to 1000)
  val firstFlow = Flow[Int].map(_ + 1)
  val secondFlow = Flow[Int].map(_ * 10)
  val sink = Sink.foreach[Int](println)

  //  akkaStream()
  //  actorEquivalent()
  //  complexOperations()
  // orderingGuarantees()
  orderingGuaranteesAsync()

  def akkaStream(): Unit = {
    //this runs on the same actor - operator/component fusion
    val graph = source.via(firstFlow).via(secondFlow).to(sink).run()
  }

  def actorEquivalent(): Unit = {
    class SimpleActor extends Actor {
      override def receive: Receive = {
        case x: Int =>
          //flow operations
          val x2 = x + 1
          val y = x2 * 10
          //sink operations
          println(y)
      }
    }

    val simpleActor = actorSystem.actorOf(Props[SimpleActor])
    (1 to 1000).foreach(simpleActor ! _)
  }

  //a single CPU core will be used for the complete processing of every single element throughout the entire flow
  //nice if operations are not expensive

  def complexOperations(): Unit = {
    val firstComplexFlow = Flow[Int].map { x =>
      Thread.sleep(1000)
      x + 1
    }

    val secondComplexFlow = Flow[Int].map { x =>
      Thread.sleep(1000)
      x * 10
    }

    val slowGraph = source.via(firstComplexFlow).via(secondComplexFlow).to(sink) //slow, 2 sec/item
    val asyncGraph = source.via(firstComplexFlow).async.via(secondComplexFlow).async.to(sink) //1 sec/item
    asyncGraph.run()

    //actor 1 executes firstComplexFlow and after that end result asynchronous to another actor
  }

  def orderingGuarantees(): Unit = {
    Source(1 to 3)
      .map(el => { println(s"Flow A: $el"); el })
      .map(el => { println(s"Flow B: $el"); el })
      .map(el => { println(s"Flow C: $el"); el })
      .runWith(Sink.ignore)

    //every element are full processed before new element is starting to process => order is guaranteed
  }

  def orderingGuaranteesAsync(): Unit = {
    Source(1 to 3)
      .map(el => { println(s"Flow A: $el"); el }).async
      .map(el => { println(s"Flow B: $el"); el }).async
      .map(el => { println(s"Flow C: $el"); el }).async
      .runWith(Sink.ignore)

    //not strong guarantees, but relative order of elements inside every component is guaranteed
    //A1 => A2 => A3
    //B1 => B2 => B3
  }
}
