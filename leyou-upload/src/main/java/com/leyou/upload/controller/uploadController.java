package com.leyou.upload.controller;

import com.leyou.upload.service.UploadService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
//@CrossOrigin
@RequestMapping("upload")
public class uploadController {

    @Autowired
    private UploadService uploadService;

    @CrossOrigin(allowCredentials="true",allowedHeaders="*",methods={RequestMethod.GET,RequestMethod.POST,RequestMethod.DELETE, RequestMethod.OPTIONS,RequestMethod.HEAD, RequestMethod.PUT, RequestMethod.PATCH}, origins="http://manage.leyou.com")
    @PostMapping("image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file){

        String url=uploadService.upload(file);
        if(StringUtils.isBlank(url)){
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }
}
