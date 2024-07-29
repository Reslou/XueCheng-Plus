package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体业务层客户端
 *
 * @author reslou
 * @date 2024/07/22
 */
@FeignClient(value = "media-api", configuration = MultipartSupportConfig.class
        , fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {
    /**
     * 上传
     *
     * @param upload     上传
     * @param objectName 对象名称
     * @return 上传文件结果dto
     */
    @RequestMapping(value = "/media/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String upload(@RequestPart("filedata") MultipartFile upload
            , @RequestParam(value = "objectName", required = false) String objectName);
}
