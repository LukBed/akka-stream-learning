package net.snipersoft.akkastream.graphs

import akka.actor.ActorSystem
import akka.stream.{BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows extends App {
  /*
  For reversable operations:
  -encrypting/decrypting
  -coding/encoding
  -serializing/deserializing
   */

  implicit val system: ActorSystem = ActorSystem("system")

  cryptography()

  def cryptography(): Unit = {
    def encrypt(n: Int)(s: String): String = s.map(c => (c + n).toChar)

    def decrypt(n: Int)(s: String): String = s.map(c => (c - n).toChar)

    val bidiCryptoStaticGraph = GraphDSL.create() { implicit builder =>
      val encryptFlowShape = builder.add(Flow[String].map(encrypt(3)(_)))
      val decryptFlowShape = builder.add(Flow[String].map(decrypt(3)(_)))

      //      BidiShape(encryptFlowShape.in, encryptFlowShape.out, decryptFlowShape.in, decryptFlowShape.out)
      BidiShape.fromFlows(encryptFlowShape, decryptFlowShape)
    }

    val unecryptedStrings = List("Akka", "is", "awesome", "testing", "bidirectional", "flows")
    val unecryptedSource = Source(unecryptedStrings)
    val encryptedStrings = unecryptedStrings.map(encrypt(3))
    val encryptedSource = Source(encryptedStrings)

    val cryptoBidiGraph = RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val unecryptedSourceShape = builder.add(unecryptedSource)
        val encryptedSourceShape = builder.add(encryptedSource)
        val bidi = builder.add(bidiCryptoStaticGraph)
        val encryptedSinkShape = builder.add(Sink.foreach[String](s =>  println(s"Encrypted: $s")))
        val decryptedSinkShape = builder.add(Sink.foreach[String](s =>  println(s"Decrypted: $s")))

        unecryptedSourceShape ~> bidi.in1 ; bidi.out1 ~> encryptedSinkShape
//        encryptedSourceShape ~> bidi.in2 ; bidi.out2 ~> decryptedSinkShape
        decryptedSinkShape <~ bidi.out2 ; bidi.in2 <~ encryptedSourceShape //better graphical representation

        ClosedShape
      }
    )

    cryptoBidiGraph.run()
  }
}
