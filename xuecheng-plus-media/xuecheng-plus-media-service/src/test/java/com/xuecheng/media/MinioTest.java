package com.xuecheng.media;

import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * minio测试
 *
 * @author 张杨
 * @date 2024/07/16
 */
public class MinioTest {

    static MinioClient minioClient = MinioClient
            .builder()
            .endpoint("http://192.168.101.65:9000")
            .credentials("minioadmin", "minioadmin")
            .build();

    @Test
    public void upload() throws Exception {
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("testbucket")
                .object("壁纸.jpg")
                .filename("E:\\yang2\\Onedrive\\照片\\KTC壁纸.jpg")
                //  .contentType(ContentInfoUtil.findMimeTypeMatch(".jpg").getMimeType())
                .build();
        minioClient.uploadObject(uploadObjectArgs);
    }

    @Test
    public void delete() throws Exception {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket("testbucket")
                .object("壁纸")
                .build();
        minioClient.removeObject(removeObjectArgs);
    }

    @Test
    public void get() throws Exception {
        FilterInputStream inputStream = minioClient
                .getObject(GetObjectArgs
                        .builder().bucket("testbucket").object("壁纸.jpg").build());
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\develop\\壁纸.jpg"));
        IOUtils.copy(inputStream, outputStream);
        FileInputStream fileInputStream = new FileInputStream(new File("E:\\yang2\\Onedrive\\照片\\KTC壁纸.jpg"));
        String s = DigestUtils.md5Hex(fileInputStream);
        FileInputStream fileInputStream1 = new FileInputStream(new File("D:\\develop\\壁纸.jpg"));
        String s1 = DigestUtils.md5Hex(fileInputStream1);
        if (s.equals(s1)) {
            System.out.println("上传成功");
        }
    }

    @Test
    public void uploadChunk() throws Exception {
        String chunkPath = "E:\\Videos\\chunk\\";
        for (int i = 0; i < 149; i++) {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .object("chunk/" + i)
                    .filename(chunkPath + i)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
        }
    }

    @Test
    public void merge() throws Exception {
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(149)
                .map(i -> ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build())
                .collect(Collectors.toList());
        ComposeObjectArgs args = ComposeObjectArgs.builder()
                .bucket("testbucket").object("merge.mp4").sources(sources).build();
        minioClient.composeObject(args);
    }
}
