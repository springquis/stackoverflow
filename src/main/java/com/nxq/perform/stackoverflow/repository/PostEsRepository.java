package com.nxq.perform.stackoverflow.repository;

import com.nxq.perform.stackoverflow.entity.es.PostEs;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostEsRepository extends ElasticsearchRepository<PostEs, Long> {
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^3\", \"body_text\", \"tags\"], \"fuzziness\": \"AUTO\"}}")
    List<PostEs> searchByKeyword(String keyword);
}
