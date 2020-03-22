package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class SpecificationService {
    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据分类id查询分组
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        List<SpecGroup> groups = specGroupMapper.select(specGroup);
        return groups;
    }

    /**
     * 根据条件查询规格参数
     * @param gid
     * @return
     */
    public List<SpecParam> queryParams(Long gid,Long cid,Boolean generic,Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setGeneric(generic);
        specParam.setSearching(searching);
        List<SpecParam> params = specParamMapper.select(specParam);
        return params;
    }

    /**
     * 新增组
     * @param group
     * @return
     */
    public int addGroup(SpecGroup group) {

        if(group.getId()==null){
            int i = specGroupMapper.insert(group);
            return i;
        }else{
            int i = specGroupMapper.updateByPrimaryKeySelective(group);
            return i;
        }
    }

    /**
     * 删除组
     * @param id
     * @return
     */
    public int deleteGroup(Long id) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setId(id);
        return specGroupMapper.delete(specGroup);
    }

    public int addParam(SpecParam param) {
        if(param.getId()==null){
            int i = specParamMapper.insert(param);
            return i;
        }else{
            int i = specParamMapper.updateByPrimaryKeySelective(param);
            return i;
        }
    }

    /**
     * 删除组
     * @param id
     * @return
     */
    public int deleteParam(Long id) {
        SpecParam specParam = new SpecParam();
        specParam.setId(id);
        return specParamMapper.delete(specParam);
    }
}
