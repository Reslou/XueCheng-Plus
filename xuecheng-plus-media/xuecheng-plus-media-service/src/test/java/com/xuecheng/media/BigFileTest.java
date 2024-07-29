package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 大文件测试
 *
 * @author 张杨
 * @date 2024/07/17
 */
public class BigFileTest {

    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("E:/Videos/黑暗荣耀01.mp4");
        String chunkPath = "E:/Videos/chunk/";
        long chunkSize = 1024 * 1024 * 5;
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        byte[] b = new byte[1024];
        RandomAccessFile r = new RandomAccessFile(sourceFile, "r");
        for (int i = 0; i < chunkNum; i++) {
            File file = new File(chunkPath + i);
            if (file.exists()) {
                file.delete();
            }
            boolean newFile = file.createNewFile();
            if (newFile) {
                RandomAccessFile rw = new RandomAccessFile(file, "rw");
                int len = -1;
                while ((len = r.read(b)) != -1) {
                    rw.write(b, 0, len);
                    if (file.length() >= chunkSize) {
                        break;
                    }
                }
                rw.close();
            }
        }
        r.close();
    }

    @Test
    public void testMerge() throws IOException {
        File chunkFolder = new File("E:/Videos/chunk/");
        File sourceFile = new File("E:/Videos/黑暗荣耀01.mp4");
        File mergeFile = new File("E:/Videos/黑暗荣耀01_2.mp4");
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        mergeFile.createNewFile();
        RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
        rw.seek(0);
        byte[] b = new byte[1024];
        File[] files = chunkFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        fileList.sort(Comparator.comparingInt(o -> Integer.parseInt(o.getName())));
        for (File file : fileList) {
            RandomAccessFile rw1 = new RandomAccessFile(file, "rw");
            int len = -1;
            while ((len = rw1.read(b)) != -1) {
                rw.write(b, 0, len);
            }
            rw1.close();
        }
        rw.close();

        FileInputStream s = new FileInputStream(sourceFile);
        FileInputStream m = new FileInputStream(mergeFile);
        String sMd5 = DigestUtils.md5Hex(s);
        String mMd5 = DigestUtils.md5Hex(m);
        if (sMd5.equals(mMd5)) {
            System.out.println("合并文件成功");
        }
    }
}
