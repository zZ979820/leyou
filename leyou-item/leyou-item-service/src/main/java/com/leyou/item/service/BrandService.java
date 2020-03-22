package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.io.File;
import java.util.List;

@Service
public class BrandService {
    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandsByPage(String key, Integer page, Integer rows, String sortBy, Boolean desc) {

        Example example = new Example(Brand.class);

        Example.Criteria criteria = example.createCriteria();
        //模糊查询
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("name","%"+key+"%").orEqualTo("letter",key);
        }

        //分页
        PageHelper.startPage(page,rows);

        //排序
        if(StringUtils.isNotBlank(sortBy)){
            example.setOrderByClause(sortBy+" "+(desc?"desc":"asc"));
        }

        List<Brand> brands = brandMapper.selectByExample(example);

        //包装成pageInfo
        PageInfo<Brand> brandPageInfo = new PageInfo<>(brands);
        //包装成分页结果集返回
        return new PageResult<>(brandPageInfo.getTotal(),brandPageInfo.getList());
    }

    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        brandMapper.insertSelective(brand);
        cids.forEach(cid->{
            brandMapper.insertCategoryAndBrand(cid,brand.getId());
        });
    }

    @Transactional
    public void delete(Brand brand) {
        String fileName=StringUtils.substringAfterLast(brand.getImage(),"/");
        int i = brandMapper.delete(brand);
        if(i==1){
            File file = new File("E:\\IDEA\\images\\leyou\\"+fileName);
            file.delete();
            brandMapper.deleteByBrandId(brand.getId());
        }

    }
    /**
     * 根据分类查询品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandsByCid(Long cid) {
        return brandMapper.queryBrandsByCid(cid);
    }

    public Brand queryBrandById(Long id) {
        return brandMapper.selectByPrimaryKey(id);
    }
}
