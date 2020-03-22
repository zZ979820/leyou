package com.leyou.search.reponsitory;

import com.leyou.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @Author Administrator
 * @Date 2020/3/15
 **/
public interface GoodsRepnsitory extends ElasticsearchRepository<Goods,Long> {
}
