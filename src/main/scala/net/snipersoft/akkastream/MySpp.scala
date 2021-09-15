package net.snipersoft.akkastream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

object MySpp extends App {
  implicit val actorSystem: ActorSystem = ActorSystem("mySystem")
  Source.single("Hello").to(Sink.foreach(println)).run()
}
