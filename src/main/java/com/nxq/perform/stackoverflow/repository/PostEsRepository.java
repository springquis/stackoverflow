package com.nxq.perform.stackoverflow.repository;

import com.nxq.perform.stackoverflow.entity.es.PostEs;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.annotations.SourceFilters;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostEsRepository extends ElasticsearchRepository<PostEs, Long> {
//    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^3\", \"body_text\", \"tags\"], \"fuzziness\": \"AUTO\"}}")
//    List<PostEs> searchByKeyword(String keyword);

    /**
     * Search full (title + body + tags)
     * LƯU Ý: tags PHẢI là text field, KHÔNG keyword
     */
    @Query("""
        {
          "multi_match": {
            "query": "?0",
            "fields": ["title^3", "body_text", "tags"],
            "fuzziness": "AUTO"
          }
        }
        """)
    List<PostEs> searchByKeyword(String keyword);

    /**
     * Search nhẹ để trả summary (id + title)
     */
    @SourceFilters(includes = {"id", "title"})
    @Query("""
    {
      "bool": {
        "should": [
          {
            "multi_match": {
              "query": "?0",
              "fields": ["title^3", "body_text"],
              "fuzziness": "AUTO"
            }
          },
          {
            "term": {
              "tags": {
                "value": "?0",
                "case_insensitive": true
              }
            }
          },
          {
            "nested": {
              "path": "comments",
              "query": {
                "match": {
                  "comments.text": "?0"
                }
              },
              "score_mode": "max"
            }
          }
        ]
      }
    }
    """)
    List<PostEs> searchForSummary(String keyword);  // hàm sử dụng
}
