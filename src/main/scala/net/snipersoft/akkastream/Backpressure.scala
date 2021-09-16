package net.snipersoft.akkastream

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

/*
elements flow as response to demand from consumer - consumer triggers the flow
if the sink is very slow, it will signal to producer to slow down producing elements
this is called backpressure and we can control it
*/
object Backpressure extends App {
  implicit val system: ActorSystem = ActorSystem("system")


  val fastSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(el => { println(s"Incoming $el"); el+1 })
  val slowSink = Sink.foreach[Int](el => { Thread.sleep(1000); println(s"Sink $el") })


//  noBackpressure()
//  backpressureExample()
//  visibleBackpressure()
  overflowStrategies()
//  throttling()

  def noBackpressure(): Unit = fastSource.to(slowSink).run() //not backpressure - one actor instance ; 1 el/sec
  def backpressureExample(): Unit = fastSource.async.to(slowSink).run() //the same result for user, but it is a backpressure

  /*
  backpressure in action - very interesting!
  default buffer is 16 elements
   */
  def visibleBackpressure(): Unit = {
    fastSource.async.via(simpleFlow).async.to(slowSink).run()
  }

  /*
  reactions to backpressure (in order):
  -try to slow down if possible (in example fastSource can, bo simpleFlow can not)
  -buffer elements until there's more demand
  -drop down elements from the buffer if it overflows => only this point we can control
  -tear down or kill the whole stream (failure)
   */

  /*
  a lot of elements will be skipped
  first 16 (default buffer from sink) and last 10 (buffer from flow) will be processed
  1-16 : no backpressure
  17-26 flow will buffer, flow will start dropping at the next element
  26-1000 flow will always drop the oldest element => 991-1000 => sink
   */
  def overflowStrategies(): Unit = {
    val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
    fastSource.async.via(bufferedFlow).async.to(slowSink).run()
  }

  /*
  Overflow strategies:
  -drop head : oldest
  -drop tail : newest
  -new : exact element to be added
  -entire buffer
  -backpressure signal
  -fail
   */

  def throttling(): Unit = {
    import scala.concurrent.duration._
    fastSource.throttle(2, 1.second).runWith(Sink.foreach(println)) //max 2 el. / sec
  }
}
