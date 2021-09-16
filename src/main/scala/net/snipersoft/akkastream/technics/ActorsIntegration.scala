package net.snipersoft.akkastream.technics

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt

object ActorsIntegration extends App {
  implicit val as: ActorSystem = ActorSystem("system")

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Got message: '$s'")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Got number: $n")
        sender() ! 2 * n
    }
  }

  val simpleActor = as.actorOf(Props[SimpleActor])
  val numberSource = Source(1 to 10)

  actorAsFlow()
  actorAsSource()

  def actorAsFlow(): Unit = {
    //based on ask pattern
    implicit val timeout: Timeout = 2.second
    //4 - how many messages can be in the actor mailbox before the actor starts backpressuring
    //ask - get Int as response
    val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

//    numberSource.via(actorBasedFlow).to(Sink.ignore).run()
    numberSource.ask[Int](parallelism = 4)(simpleActor).run() //shorter
  }

  def actorAsSource(): Unit = {
    val actorPoweredSource = Source.actorRef(bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
    val materializedActorRef: ActorRef = actorPoweredSource.to(Sink.foreach[Int](n => println(s"Got $n"))).run()
    materializedActorRef ! 10
    materializedActorRef ! akka.actor.Status.Success("complete") //terminate
  }
}
