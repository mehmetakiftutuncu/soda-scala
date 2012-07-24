package com.socrata
package consumer
package http

import scala.io.Codec

import java.net.{URL, URLEncoder}

import com.ning.http.client.AsyncHttpClient

import `consumer-impl`.implicits._
import com.socrata.http.{StandardConsumer, BodyConsumer, Retryable}
import iteratee.CharIteratee
import future.Future

class LowLevelHttp(val client: AsyncHttpClient, val host: String, val port: Int, val authorization: Authorization) extends LowLevel {
  import LowLevelHttp._

  def execute[T](resource: Resource, getParameters: Map[String, Seq[String]], iteratee: CharIteratee[T]): Future[T] = {
    def bodyConsumer(codec: Codec) = new RowProducer(codec, iteratee)
    val builder = client.prepareGet(new URL("https", host, port, buildFile(resource, getParameters)).toString).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      authorize(authorization)
    builder.makeRequest(new StandardConsumer(builder, authorization, bodyConsumer)).flatMap(maybeRetry)
  }

  def maybeRetry[T](x: Retryable[T]): Future[T] = x match {
    case Left(retry) => error("NYI")
    case Right(result) => Future(result)
  }
}

object LowLevelHttp {
  def encode(s: String) = URLEncoder.encode(s, "UTF-8")

  def buildFile(resource: Resource, parameters: Map[String, Seq[String]]) = {
    val filename = "/id/" + resource.name
    if(parameters.isEmpty) filename
    else filename + "?" + parameters.iterator.flatMap { case (k,vs) => vs.map { v => encode(k) + "=" + encode(v) } }.mkString("&")
  }
}
