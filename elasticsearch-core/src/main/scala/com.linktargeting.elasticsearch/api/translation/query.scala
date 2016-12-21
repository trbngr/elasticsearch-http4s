package com.linktargeting.elasticsearch.api.translation

import com.linktargeting.elasticsearch.api._

object query {

  private[translation] object QueryMapper extends DataMapper[Query] {
    def data(query: Query) = {
      val options = query match {
        case x: QueryLike ⇒ Map.empty ++ x.options.size.map("size" → _)
        case _            ⇒ Map.empty[String, Any]
      }
      val queryData = Map("query" → (query match {
        case x: PrefixQuery ⇒ PrefixQueryMapper.data(x)
      }))
      Map(queryData.toSeq ++ options.toSeq: _*)
    }
  }

  private[translation] object PrefixQueryMapper extends DataMapper[PrefixQuery] {
    override def data(x: PrefixQuery) = x match {
      case PrefixQuery(field, value, boost, _) if boost > 0 ⇒ Map("prefix" → Map(field → Map("value" → value, "boost" → boost)))
      case PrefixQuery(field, value, _, _)                  ⇒ Map("prefix" → Map(field -> value))
    }
  }
}