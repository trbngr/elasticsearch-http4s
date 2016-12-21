package com.linktargeting.elasticsearch.http.circe

import cats.syntax.either._
import com.linktargeting.elasticsearch.api._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Json}

private[circe] object codecs {

  import syntax._

  implicit val indexEncoder = deriveEncoder[Idx]
  implicit val idEncoder = deriveEncoder[Id]
  implicit val typeEncoder = deriveEncoder[Type]
  implicit val errorEncoder = deriveEncoder[EsError]
  implicit val errorDecoder = deriveDecoder[EsError]

  implicit val shardsEncoder = deriveEncoder[Shards]
  implicit val shardsDecoder = deriveDecoder[Shards]

  implicit val responseEncoder = deriveEncoder[Response]
  implicit val responseDecoder = Decoder.decodeJson.map[Response] { json ⇒
    Response(
      shards = json \ "_shards" shards,
      index = json \ "_index" string "",
      `type` = json \ "_type" string "",
      id = json \ "_id" string "",
      version = json \ "_version" int 0
    )
  }

  implicit val indexResponseEncoder = deriveEncoder[IndexResponse]
  implicit val indexResponseDecoder = deriveDecoder[IndexResponse]

  implicit val deleteIndexResponseEncoder = deriveEncoder[DeleteIndexResponse]
  implicit val deleteIndexResponseDecoder = deriveDecoder[DeleteIndexResponse]

  implicit val refreshResponseEncoder = deriveEncoder[RefreshResponse]
  implicit val refreshResponseDecoder = Decoder.decodeJson map[RefreshResponse] { json ⇒
    RefreshResponse(shards = json \ "_shards" shards)
  }

  implicit val bulkActionEncoder = Encoder.encodeString.contramap[BulkAction] {
    case BulkIndex  ⇒ "index"
    case BulkCreate ⇒ "create"
    case BulkDelete ⇒ "delete"
    case BulkUpdate ⇒ "update"
  }

  implicit val bulkResponseEncoder = deriveEncoder[BulkResponse]
  implicit val bulkResponseDecoder = Decoder.decodeJson map { json ⇒
    val actionName = json.hcursor.fields.map(_.head).getOrElse(throw new RuntimeException("can't find bulk action name"))
    BulkResponse(
      action = actionName match {
        case "index"  ⇒ BulkIndex
        case "create" ⇒ BulkCreate
        case "update" ⇒ BulkUpdate
        case "delete" ⇒ BulkDelete
        case other    ⇒ throw new RuntimeException(s"Invalid bulk action: '$other'")
      },
      status = json \ actionName \ "status" int 0,
      response = json \ actionName response
    )
  }

  implicit val jsonDocEncoder = deriveEncoder[JsonDocument[Json]]
  implicit val jsonDocDecoder = Decoder.decodeJson map { json ⇒
    JsonDocument(
      index = Idx(json \ "_index" string ""),
      `type` = Type(json \ "_type" string ""),
      id = Id(json \ "_id" string ""),
      source = json \ "_source" json
    )
  }

  implicit val searchResponseEncoder = deriveEncoder[SearchResponse[Json]]
  implicit val searchResponseDecoder = Decoder.decodeJson map { json ⇒
    SearchResponse(
      shards = json \ "_shards" shards,
      total = json \ "hits" \ "total" int 0,
      documents = json \ "hits" \ "hits" documents
    )
  }

  implicit val scrollResponseEncoder = deriveEncoder[ScrollResponse[Json]]
  implicit val scrollResponseDecoder = Decoder.decodeJson map { json ⇒
    ScrollResponse(
      scrollId = json \ "_scroll_id" string "",
      results = json.as[SearchResponse[Json]].getOrElse(SearchResponse[Json](Shards(0, 0, 0), 0, Seq.empty))
    )
  }
}
