package com.graysan.elasticsearch.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.graysan.elasticsearch.dto.SearchRequestDto;
import com.graysan.elasticsearch.model.Item;
import com.graysan.elasticsearch.repository.ItemRepository;
import com.graysan.elasticsearch.util.ESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final JsonDataService jsonDataService;
    private final ElasticsearchClient elasticsearchClient;

    public Item createIndex(Item item) {
        return itemRepository.save(item);
    }

    public void addItemsFromJson() {
        log.info("Adding items from JSON");
        List<Item> itemList = jsonDataService.readItemsFromJson();
        itemRepository.saveAll(itemList);

    }

    public List<Item> getAllDataFromIndex(String indexName) {
        var query = ESUtil.createMatchAllQuery();
        log.info("ElasticSearch query: {}", query.toString());
        SearchResponse<Item> response = null;
        try {
             response = elasticsearchClient.search(
                q -> q.index(indexName).query(query), Item.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("ElasticSearch response: {}", response.toString());
        return extractItemsFromResponse(response);

    }

    public List<Item> extractItemsFromResponse(SearchResponse<Item> response) {
        return response.hits()
                .hits()
                .stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    public List<Item> searchItemsByFieldAndValue(SearchRequestDto dto) {
        Supplier<Query> query = ESUtil.buildQueryForFieldAndValue(dto.getFieldName().get(0),
                dto.getSearchValue().get(0));
        log.info("ElasticSearch query: {}", query.toString());
        SearchResponse<Item> response = null;
        try {
            response = elasticsearchClient.search(
                    q -> q.index("items_index").query(query.get()), Item.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("ElasticSearch response: {}", response.toString());
        return extractItemsFromResponse(response);
    }


}
