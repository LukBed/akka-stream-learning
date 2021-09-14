package net.snipersoft.akkastream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object MySpp extends App {
  implicit val actorSystem: ActorSystem = ActorSystem("mySystem")
  implicit val materializer: ActorMaterializer.type = ActorMaterializer
  Source.single("Hello").to(Sink.foreach(println)).run()
}
