package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.pojo.Stock;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.cache.TransactionalCacheManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private DataSourceTransactionManager transactionManager;
    /**
     * 查询商品
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    public PageResult<SpuBo> querySpuBoByPage(String key, Boolean saleable, Integer page, Integer rows) {
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();

        // 搜索条件
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title","%"+key+"%");
        }
        if(saleable!=null){
            criteria.andEqualTo("saleable",saleable);
        }
        // 分页条件
        PageHelper.startPage(page,rows);
        // 执行查询
        List<Spu> spus = spuMapper.selectByExample(example);
        PageInfo<Spu> pageInfo=new PageInfo<>(spus);
        List<SpuBo> spuBos = spus.stream().map(spu -> {
            SpuBo spuBo = new SpuBo();
            //copy共同属性的值到新的对象
            BeanUtils.copyProperties(spu, spuBo);
            //查询分类名称
            List<String> names = categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            spuBo.setCname(StringUtils.join(names, "-"));
            //查询品牌名称
            spuBo.setBname(brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());
            return spuBo;
        }).collect(Collectors.toList());


        return new PageResult<>(pageInfo.getTotal(),spuBos);
    }
    /**
     * 新增商品
     * @param spuBo
     * @return
     */
    @Transactional
    public void saveGoods(SpuBo spuBo) {
        DefaultTransactionDefinition dtd = new DefaultTransactionDefinition();
        dtd.setName("");
        dtd.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(dtd);
        try {
            //新增spu
            spuBo.setId(null);
            spuBo.setSaleable(true);
            spuBo.setValid(true);
            spuBo.setCreateTime(new Date());
            spuBo.setLastUpdateTime(spuBo.getCreateTime());
            spuMapper.insertSelective(spuBo);
            //新增spuDetail
            SpuDetail spuDetail = spuBo.getSpuDetail();
            spuDetail.setSpuId(spuBo.getId());
            spuDetailMapper.insertSelective(spuDetail);

            saveSkuAndStock(spuBo);
        }catch (Exception e){
            transactionManager.rollback(status);
        }
    }
    /**
     * 修改商品
     * @param spuBo
     */
    public void updateGoods(SpuBo spuBo) {
        //获取sku
        Sku t = new Sku();
        t.setSpuId(spuBo.getId());
        List<Sku> skus = skuMapper.select(t);
        if(!CollectionUtils.isEmpty(skus)){
            skus.forEach(sku->{
                //删除stock
                stockMapper.deleteByPrimaryKey(sku.getId());
            });
        }
        //删除sku
        Sku sku=new Sku();
        sku.setSpuId(spuBo.getId());
        skuMapper.delete(sku);

        //新增sku和库存
        saveSkuAndStock(spuBo);

        spuBo.setCreateTime(null);
        spuBo.setLastUpdateTime(new Date());
        spuBo.setValid(null);
        spuBo.setSaleable(null);
        //修改spu
        spuMapper.updateByPrimaryKeySelective(spuBo);
        //修改spuDetail
        spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());
    }

    private void saveSkuAndStock(SpuBo spuBo) {
        spuBo.getSkus().forEach(sku->{
            //新增sku
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insertSelective(sku);
            //新增库存
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockMapper.insertSelective(stock);
        });
    }

    /**
     * 根据spuId查询spuDetail
     * @param spuId
     * @return
     */
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        return spuDetailMapper.selectByPrimaryKey(spuId);
    }
    /**
     * 根据spuId查询sku的集合
     * @param spuId
     * @return
     */
    public List<Sku> querySkusBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(sku);
        skus.forEach(s->{
            s.setStock(stockMapper.selectByPrimaryKey(s.getId()).getStock());
        });
        return skus;
    }

    /**
     * 删除商品
     * @param spuBo
     */
    public void deleteGoods(SpuBo spuBo) {
        spuBo.setValid(false);
        spuMapper.updateByPrimaryKeySelective(spuBo);
    }

    /**
     * 上下架商品
     * @param spuBo
     */
    public void saleableGoods(SpuBo spuBo) {
        Boolean saleable = spuBo.getSaleable();
        if (saleable){
            spuBo.setSaleable(false);
        }else {
            spuBo.setSaleable(true);
        }
        spuMapper.updateByPrimaryKeySelective(spuBo);
    }
}
