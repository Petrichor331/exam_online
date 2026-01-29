package com.example.controller;


import cn.hutool.core.io.FileUtil;
import com.example.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@CrossOrigin
@RestController
@RequestMapping("/files")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final String filePath = System.getProperty("user.dir") + "/files/";

    /*
    冒号后面是默认值，此时为空，如果fileBaseUrl为空，则使用默认值
     */
    @Value("${fileBaseUrl:}")
    private String fileBaseUrl;

    @PostMapping("/upload")
    public Result upload(MultipartFile file){
        String fileName = file.getOriginalFilename();
        try{
            if(!FileUtil.isDirectory(filePath)){
                FileUtil.mkdir(filePath);
            }
            fileName = System.currentTimeMillis() + "-" + fileName;
            String realFilePath = filePath + fileName;
            FileUtil.writeBytes(file.getBytes(),realFilePath);

        } catch (IOException e) {
            logger.error(fileName + "文件上传失败",e);
        }
        String url = fileBaseUrl + "/files/download/" +fileName;
        return Result.success(url);
    }

    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response){
        OutputStream os;
        try{
            if(fileName != null){
                /*在响应头中编码filename，防止中文乱码，这里是用户下载的时候看到的文件名*/
                response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                /*告诉浏览器返回的数据类型是二进制数据,直接下载就好了*/
                response.setContentType("application/octet-stream");
                byte[] bytes = FileUtil.readBytes(filePath+fileName);
                /*获取到浏览器的通道*/
                os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
            }
        } catch (Exception e) {
            logger.error("文件下载失败",fileName);
        }
    }

}
