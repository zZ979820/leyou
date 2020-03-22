package com.leyou.item.mapper;

import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends Mapper<Brand> {

    /**
     * 添加品牌
     * @param cid
     * @param bid
     */
    @Insert("insert into tb_category_brand(category_id,brand_id) values(#{cid},#{bid}) ")
    void insertCategoryAndBrand(@Param("cid") Long cid,@Param("bid") Long bid);

    /**
     * 删除品牌
     * @param id
     */
    @Delete("delete from tb_category_brand where brand_id =#{id} ")
    void deleteByBrandId(@Param("id") Long id);

    /**
     * 根据分类获取品牌
     * @param cid
     * @return
     */
    @Select("select * from tb_category_brand cb join tb_brand b on cb.brand_id=b.id where cb.category_id=#{cid}")
    List<Brand> queryBrandsByCid(@Param("cid") Long cid);
}
