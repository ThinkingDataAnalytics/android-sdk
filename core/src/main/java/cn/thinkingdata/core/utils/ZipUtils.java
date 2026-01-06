/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author liulongbing
 * @since 2024/5/22
 */
public class ZipUtils {

    public static String gzip(String rawMessage) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes().length);
            GZIPOutputStream gos = new GZIPOutputStream(os);
            gos.write(rawMessage.getBytes());
            gos.close();
            byte[] compressed = os.toByteArray();
            os.close();
            return new String(Base64Coder.encode(compressed));
        } catch (IOException e) {
            return "";
        }
    }

    public static String unGzip(String encodeData) {
        try {
            byte[] compressedData = Base64Coder.decode(encodeData);
            ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
            GZIPInputStream gis = new GZIPInputStream(bis);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            gis.close();
            bis.close();
            bos.close();
            return bos.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
