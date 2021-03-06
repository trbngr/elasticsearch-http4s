package dutchman.document

import cats.data.EitherT
import dutchman.{ApiSpecs, ElasticOps}
import dutchman.dsl._
import dutchman.model._
import dutchman.ops.{deleteIndex, _}

trait IndexSpecs[Json] {
  this: ApiSpecs[Json] ⇒

  private[this] val idx: Idx = "index_specs"
  private[this] val tpe: Type = "person"

  "Index" when {
    val person = Person("123", "Chris", "PHX")

    "it doesn't exist" should {
      "be created" in {
        val api: EitherT[ElasticOps, ESError, _root_.dutchman.dsl.IndexResponse] = for {
          r ← index(idx, tpe, person, None)
          _ ← deleteIndex(idx)
        } yield r

        val response = client(api.value).futureValue
        response match {
          case Left(e) ⇒ fail(e.reason)
          case Right(r) ⇒ r.created shouldBe true
        }
      }
    }

    "It already exists" should {
      "be not created" in {

        val api = for {
          _ ← index(idx, tpe, person, None)
          r ← index(idx, tpe, person, None)
          _ ← deleteIndex(idx)
        } yield r

        val response = client(api.value).futureValue
        response match {
          case Left(e) ⇒ fail(e.reason)
          case Right(r) ⇒ r.created shouldBe false
        }
      }
    }
  }
}
