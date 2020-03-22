package com.leyou.item.api;

import com.leyou.item.pojo.SpecParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("spec")
public interface SpecificationApi {

    /**
     * 根据条件查询规格参数
     * @param gid
     * @return
     */
    @GetMapping("params")
    public List<SpecParam> queryParams(
            @RequestParam(value = "gid",required = false) Long gid,
            @RequestParam(value ="cid",required = false) Long cid,
            @RequestParam(value ="generic",required = false) Boolean generic,
            @RequestParam(value ="searching",required = false) Boolean searching);

}