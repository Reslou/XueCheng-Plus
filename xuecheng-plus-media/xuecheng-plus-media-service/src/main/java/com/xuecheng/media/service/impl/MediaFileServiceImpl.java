package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MediaFileServiceImpl implements MediaFileService {

    private final MediaFilesMapper mediaFilesMapper;
    private final MediaProcessMapper mediaProcessMapper;
    private final MinioClient minioClient;

    @Autowired
    MediaFileService currentProxy;

    //普通文件桶
    @Value("${minio.bucket.files}")
    private String bucket_Files;

    //普通文件桶
    @Value("${minio.bucket.videofiles}")
    private String bucket_videoFiles;

    /**
     * 查询媒体字段
     *
     * @param companyId           公司标识
     * @param pageParams          页面参数
     * @param queryMediaParamsDto 查询媒体参数
     * @return 页面结果<media files>
     */
    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        return new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());

    }


    /**
     * 上传文件
     *
     * @param companyId           公司标识
     * @param uploadFileParamsDto 上传文件参数
     * @param localFilePath       本地文件路径
     * @param objectName          对象名称
     * @return 上传文件结果dto
     */
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto
            , String localFilePath, String objectName) {
        //上传文件到 MinIO
        String filename = uploadFileParamsDto.getFilename();//文件名
        String extension = filename.substring(filename.lastIndexOf("."));//拓展名
        String mimeType = getMimeType(extension);
        String defaultFolderPath = getDefaultFolderPath();//文件的默认目录
        String fileMd5 = getFileMd5(new File(localFilePath));
        if (StringUtils.isEmpty(objectName)) {
            objectName = defaultFolderPath + fileMd5 + extension;//存储到minio中的对象名(带目录)
        }
        boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_Files, objectName);
        if (!result) {
            XueChengPlusException.cast("上传失败");
        }

        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_Files, objectName);

        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }


    /**
     * 检查文件
     *
     * @param fileMd5 文件md5
     * @return Rest响应<boolean>
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            InputStream stream;
            try {
                stream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(mediaFiles.getBucket())
                        .object(mediaFiles.getFilePath())
                        .build());
                if (stream != null) {
                    //文件已存在
                    stream.close();
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                log.error("检查文件失败:{}", e.getMessage());
            }
        }
        //文件不存在
        return RestResponse.success(false);
    }

    /**
     * 检查分块文件
     *
     * @param fileMd5    文件md5
     * @param chunkIndex 分块文件索引
     * @return Rest响应<boolean>
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        String chunkFilePath = chunkFileFolderPath + chunkIndex;
        InputStream stream;
        try {
            stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket_videoFiles)
                    .object(chunkFilePath)
                    .build());
            if (stream != null) {
                //分块文件已存在
                stream.close();
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            log.error("检查分块文件失败:{}", e.getMessage());
        }
        //分块文件不存在
        return RestResponse.success(false);
    }

    /**
     * 上传分块文件
     *
     * @param fileMd5            文件md5
     * @param chunk              分块文件
     * @param localChunkFilePath 当地分块文件文件路径
     * @return
     */
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        String chunkFilePath = chunkFileFolderPath + chunk;
        String mimeType = getMimeType(null);
        boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_videoFiles, chunkFilePath);
        if (!b) {
            return RestResponse.validfail(false, "上传分块文件失败");
        }
        return RestResponse.success(true);
    }

    /**
     * 合并分块文件
     *
     * @param companyId           公司标识
     * @param fileMd5             文件md5
     * @param chunkTotal          分块文件总计
     * @param uploadFileParamsDto 上传文件参数
     * @return 其他反应
     */
    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //合并分块文件
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        List<ComposeSource> sources = Stream
                .iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_videoFiles)
                        .object(chunkFileFolderPath.concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());
        String filename = uploadFileParamsDto.getFilename();
        String extName = filename.substring(filename.lastIndexOf("."));
        String mergeFilePath = getFilePathByMd5(fileMd5, extName);
        try {
            ObjectWriteResponse response = minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket_videoFiles)
                            .object(mergeFilePath)
                            .sources(sources).build());
        } catch (Exception e) {
            log.error("合并文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage());
            return RestResponse.validfail(false, "合并文件失败");
        }
        log.debug("合并文件成功:{}", mergeFilePath);

        //验证md5
        File minioFile = downloadFileFromMinIO(bucket_videoFiles, mergeFilePath);
        if (minioFile == null) {
            log.error("下载合并后文件失败,mergeFilePath:{}", mergeFilePath);
            return RestResponse.validfail(false, "下载合并后文件失败。");
        }
        uploadFileParamsDto.setFileSize(minioFile.length());
        try (FileInputStream inputStream = new FileInputStream(minioFile)) {
            String md5Hex = DigestUtils.md5Hex(inputStream);
            if (!fileMd5.equals(md5Hex)) {
                return RestResponse.validfail(false, "合并后文件校验失败");
            }
            uploadFileParamsDto.setFileSize(minioFile.length());
        } catch (Exception e) {
            log.error("合并后文件校验失败,fileMd5:{},异常:{}", fileMd5, e.getMessage());
            return RestResponse.validfail(false, "合并后文件校验失败");
        } finally {
            minioFile.delete();
        }
        //文件入库
        currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_videoFiles, mergeFilePath);
        //清除分块文件
        clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true);
    }

    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjectList = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());
            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                    .bucket(bucket_videoFiles)
                    .objects(deleteObjectList).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            for (Result<DeleteError> result : results) {
                DeleteError error = new DeleteError();
                try {
                    error = result.get();
                } catch (Exception e) {
                    log.error("清除分块文件失败,objectName:{}", error.objectName());
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("清除分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath);
        }
    }

    /**
     * 从minio下载文件
     *
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 得到合并后的文件的地址
     *
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return 合并后的文件的地址
     */
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    /**
     * 将媒体文件添加到数据库中
     *
     * @param companyId           公司标识
     * @param fileMd5             文件md5
     * @param uploadFileParamsDto 上传文件参数
     * @param bucket              桶
     * @param objectName          对象名称
     * @return <p>
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");

            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert < 0) {
                log.error("保存文件到数据库失败,{}", mediaFiles);
                XueChengPlusException.cast("保存文件到数据库失败");
            }
            addWaitingTask(mediaFiles);
            log.info("保存文件到数据库成功,{}", mediaFiles);
        }
        return mediaFiles;
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date()).replace("-", "/") + "/";
    }

    //获取文件的md5
    @SuppressWarnings("CallToPrintStackTrace")
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取mime类型
     *
     * @param extension 扩展
     * @return mime类型
     */
    private String getMimeType(String extension) {
        if (extension == null)
            extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    /**
     * @param localFilePath 文件地址
     * @param bucket        桶
     * @param objectName    对象名称
     * @description 将文件写入minIO
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(testbucket);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            XueChengPlusException.cast("上传文件到文件系统失败");
            return false;
        }
        return true;
    }

    /**
     * 按id获取文件
     *
     * @param mediaId 媒体id
     * @return <p>
     */
    @Override
    public MediaFiles getFileById(String mediaId) {
        return mediaFilesMapper.selectById(mediaId);
    }

    private void addWaitingTask(MediaFiles mediaFiles) {
        String filename = mediaFiles.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        if (mimeType.equals("video/x-msvideo")) {
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setFailCount(0);
            mediaProcess.setUrl(null);
            mediaProcessMapper.insert(mediaProcess);
        }
    }
}
