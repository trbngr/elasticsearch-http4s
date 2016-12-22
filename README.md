# Elasticsearch HTTP for Scala


Easy to use HTTP Elasticsearch client with a nice Scala DSL.

Will add more docs soon.

## Usage
```scala
package io.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.linktargeting.elasticsearch.api._
import com.linktargeting.elasticsearch.api.translation._
import com.linktargeting.elasticsearch.dsl._
import com.linktargeting.elasticsearch.{AkkaHttpClient, http}
import io.circe.Json

object Example extends App {

  import http.{Endpoint, _}
  import http.circe._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val endpoint = Endpoint.localhost

  //Raw client only returns JSON
  val httpClient = AkkaHttpClient()

  httpClient.execute(endpoint, NullRequestSigner)(Request(GET, "/twitter/tweet/_search", Map("q" → "user:kimchy"))) map { (json: Json) ⇒
    println(s"response: $json")
  }

  //That kind of sucks. This isn't Java ;)
  //Let's use the DSL
  implicit val dsl: Dsl[Json] = httpClient.bind(endpoint)

  //You can use the DSL directly via it's different apis.
  dsl.document
  dsl.indices
  dsl.search

  //Or just use it implicitly.

  val query = PrefixQuery("user", "kimchy")

  val longFormApi = Search(Idx("twitter"), Type("tweet"), query)
  //or save some keystrokes
  val api = Search("twitter", "tweet", query)

  api { (response: SearchResponse[Json]) ⇒
    println(s"total: ${response.total}")
    println(s"shards: ${response.shards}")

    response.documents foreach { (document: JsonDocument[Json]) ⇒
      val index: Idx = document.index
      val tpe: Type = document.`type`
      val id: Id = document.id

      //we now have our Json in the format that we prefer. Circe in this example. More support on the way
      val json: Json = document.source

      println(s"json: $index")
      println(s"json: $tpe")
      println(s"json: $id")
      println(s"json: $json")
    }
  }

  Index("people", "person", Document("1", Map("name" → "Chris"))) map { (r: IndexResponse) ⇒ println(s"Indexed: $r") }

  //That Document is necessary but you can bring your own classes too
  case class Person(id: Int, name: String)
  implicit val personDocument = new ESDocument[Person] {
    def document(a: Person) = Document(a.id.toString, Map("name" → a.name))
  }

  Index("people", "person", Person(1, "Christopher")) map { (r: IndexResponse) ⇒ println(s"Indexed: $r") }

  //IndexApi
  Refresh("people") { (r: RefreshResponse) ⇒
    println(s"shards: ${r.shards}")
  }

  //Bulk
  val actions = Seq(
    Bulk(Delete("people", "person", "1", version = None)),
    Bulk(Index("people", "person", Person(2, "Hailey"))),
    Bulk(Index("people", "person", Person(3, "Misty")))
  )

  Bulk(actions: _*) { (responses: Seq[BulkResponse]) ⇒
    responses foreach { (response: BulkResponse) ⇒
      val status: Int = response.status
      val action: BulkAction = response.action
    }
  }

}

```