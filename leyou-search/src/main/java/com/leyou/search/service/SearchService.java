package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.reponsitory.GoodsRepnsitory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Administrator
 * @Date 2020/3/13
 **/
@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsRepnsitory goodsRepnsitory;
    private static final ObjectMapper MAPPER=new ObjectMapper();

    public SearchResult search(SearchRequest request) {
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if(StringUtils.isBlank(request.getKey())){
            return null;
        }
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        QueryBuilder basicQuery = QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND);
        // 1、对key进行全文检索查询
        queryBuilder.withQuery(basicQuery);
        // 2、通过sourceFilter设置返回的结果字段,我们只需要id、skus、subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        // 3、分页
        queryBuilder.withPageable(PageRequest.of(request.getPage()-1,request.getSize()));
        // 4、排序
        String sortBy = request.getSortBy();
        Boolean desc = request.getDescending();
        if(StringUtils.isNotBlank(sortBy)){
            //如果不为空，则进行排序
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(desc?SortOrder.DESC : SortOrder.ASC));
        }
        String categoryAggName = "categories";
        String brandAggName = "brands";
        //添加聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        // 查询，获取结果
        AggregatedPage<Goods> goods = (AggregatedPage<Goods>)goodsRepnsitory.search(queryBuilder.build());
        //获取分类聚合结果集
        List<Map<String,Object>> categories=getCategoryAggResult(goods.getAggregation(categoryAggName));
        //获取品牌聚合结果集
        List<Brand> brands=getBrandAggResult(goods.getAggregation(brandAggName));
        //判断分类聚合的结果集大小，等于1则聚合
        List<Map<String,Object>> specs=null;
        if(!CollectionUtils.isEmpty(categories)&&categories.size()==1){
            specs=getParamAggResult((Long)categories.get(0).get("id"),basicQuery);
        }

        // 封装结果并返回
        return new SearchResult(goods.getTotalElements(),goods.getTotalPages(),goods.getContent(),categories,brands,specs);
    }

    /**
     * 聚合出规格参数过滤条件
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getParamAggResult(Long cid, QueryBuilder basicQuery) {
        //创建自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //基于基本的查询条件，聚合规格参数
        queryBuilder.withQuery(basicQuery);
        //查询要聚合的规格参数
        List<SpecParam> params = specificationClient.queryParams(null, cid, null, true);
        params.forEach(param->{
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs."+param.getName()+".keyword"));
        });
        //只需要聚合结果集，不需要查询结果集
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{},null));
        //执行聚合查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)goodsRepnsitory.search(queryBuilder.build());
        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
        //定义一个集合，收集聚合结果集
        List<Map<String,Object>> specs=new ArrayList<>();
        for (Map.Entry<String,Aggregation> entry:aggregationMap.entrySet()){
            Map<String,Object> map=new HashMap<>();
            //放入规格参数名
            map.put("k",entry.getKey());
            Aggregation value = entry.getValue();
            //收集规格参数值
            //遍历每个聚合中桶，把桶中key放入收集规格参数的集合中
            List<Object> options=((StringTerms)entry.getValue()).getBuckets().stream().map(bucket -> {
                return bucket.getKeyAsString();
            }).collect(Collectors.toList());
            map.put("options",options);
            specs.add(map);
        }
        return specs;
    }

    /**
     * 解析品牌聚合结果集
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        return ((LongTerms)aggregation).getBuckets().stream().map(bucket -> {
            return brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
        }).collect(Collectors.toList());
    }

    /**
     * 解析分类聚合结果集
     * @param aggregation
     * @return
     */
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        return ((LongTerms)aggregation).getBuckets().stream().map(bucket -> {
            Map<String,Object> map=new HashMap<>();
            String name = categoryClient.queryNamesByIds(Arrays.asList(bucket.getKeyAsNumber().longValue())).get(0);
            map.put("id",bucket.getKeyAsNumber().longValue());
            map.put("name",name);
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 构建商品
     * @param spu
     * @return
     * @throws IOException
     */
    public Goods buildGoods(Spu spu) throws IOException {

        //根据分类id查询分类名称
        List<String> names = categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        //根据品牌Id查询品牌名称
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        //根据spuId查询所有的sku
        List<Sku> skus = goodsClient.querySkusBySpuId(spu.getId());
        //初始化一个价格集合，收集所有的sku的价格
        List<Long> prices=new ArrayList<>();
        //收集sku的必要字段信息
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> map = new HashMap<>();
            map.put("id",sku.getId());
            map.put("title",sku.getTitle());
            map.put("price",sku.getPrice());
            //获取sku中的图片，数据库的图片可能是多张，多张以,号分隔，获取第一张图片
            map.put("image",StringUtils.isBlank(sku.getImages())?"":StringUtils.split(sku.getImages(),",")[0]);
            list.add(map);
        });
        //查询出所有的搜索规格参数
        List<SpecParam> params = specificationClient.queryParams(null, spu.getCid3(), null, true);
        //查询spuDetail，获取规格参数值
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spu.getId());
        //获取通用的规格参数
        Map<String,Object> genericSpecMap=MAPPER.readValue(spuDetail.getGenericSpec(),new TypeReference<Map<String, Object>>(){});
        //获取特殊的规格参数
        Map<String,List<Object>> specialSpecMap=MAPPER.readValue(spuDetail.getSpecialSpec(),new TypeReference<Map<String,List<Object>>>(){});
        //定义map接收｛规格参数名，规格参数值｝
        Map<String,Object> paramMap=new HashMap<>();
        params.forEach(param->{
            //判断是否通用规格参数
            if(param.getGeneric()){
                //获取通用规格参数值
                String value = genericSpecMap.get(param.getId().toString()).toString();
                //判断是否是数值类型
                if(param.getNumeric()){
                    //如果是数值的话，判断该数值落在哪个区间
                    value = chooseSegment(value, param);
                }
                paramMap.put(param.getName(),value);
            }else {
                paramMap.put(param.getName(),specialSpecMap.get(param.getId().toString()));
            }
        });
        Goods goods = new Goods();
        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        //拼接all字段，需要分类名称以及品牌名称
        goods.setAll(spu.getTitle()+" "+ StringUtils.join(names," ") +" "+brand.getName());


        //获取spu下的所有sku的价格
        goods.setPrice(prices);
        //获取spu下的所有sku,并转化成json字符串
        goods.setSkus(MAPPER.writeValueAsString(list));
        //获取所有查询的规格参数{name:value}
        goods.setSpecs(paramMap);
        return goods;
    }
    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }
}
