package com.leespy.http;

import com.fasterxml.jackson.databind.JavaType;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.leespy.json.Jsoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.Map;

/**
 * Http请求服务类
 * <p/>
 * Date: 16/5/6
 * Time: 下午3:47
 *
 * @author i@leespy.com
 */
public class Http {

    private static final Logger logger = LoggerFactory.getLogger(Http.class);

    private String url;

    private HttpMethod method = HttpMethod.GET;

    private Map<String, String> headers = Collections.emptyMap();

    private Map<String, ?> params = Collections.emptyMap();

    /**
     * 请求body
     */
    private String body;

    private Boolean ssl = Boolean.FALSE;

    private Integer connectTimeout = 1000 * 5;

    private Integer readTimeout = 1000 * 5;

    /**
     * 是否编码
     */
    private Boolean encode = Boolean.TRUE;

    /**
     * 请求内容类型
     */
    private String contentType = "";

    /**
     * 编码字符集
     */
    private String charset = HttpRequest.CHARSET_UTF8;

    /**
     * 接收类型
     */
    private String accept = "";

    /**
     * 是否接受gzip
     */
    private Boolean acceptGzip = Boolean.TRUE;

    private Http(String url) {
        this.url = url;
    }

    private Http method(HttpMethod method) {
        this.method = method;
        return this;
    }

    public Http ssl() {
        this.ssl = Boolean.TRUE;
        return this;
    }

    public Http headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public Http params(Map<String, ?> params) {
        this.params = params;
        return this;
    }

    /**
     * 请求body
     */
    public Http body(String body) {
        this.body = body;
        return this;
    }

    public Http encode(Boolean encode) {
        this.encode = encode;
        return this;
    }

    public Http contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Http charset(String charset) {
        this.charset = charset;
        return this;
    }

    public Http accept(String accept) {
        this.accept = accept;
        return this;
    }

    public Http acceptGzip(Boolean acceptGzip) {
        this.acceptGzip = acceptGzip;
        return this;
    }

    /**
     * set connect timeout
     *
     * @param connectTimeout (s)
     */
    public Http connTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout * 1000;
        return this;
    }

    /**
     * set read timeout
     *
     * @param readTimeout (s)
     */
    public Http readTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout * 1000;
        return this;
    }

    public String request() {
        switch (method) {
            case GET:
                return doGet();
            case POST:
                return doPost();
            default:
                break;
        }
        return null;
    }

    public <T> T requestJson(Class<T> clazz) {
        return Jsoner.DEFAULT.fromJson(request(), clazz);
    }

    public <T> T requestType(JavaType type) {
        return Jsoner.DEFAULT.fromJson(request(), type);
    }

    private String doPost() {
        HttpRequest post = HttpRequest.post(url, params, encode)
                .headers(headers)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .uncompress(true);

        if (acceptGzip) {
            post.acceptGzipEncoding();
        }

        setOptionalHeaders(post);

        if (!Strings.isNullOrEmpty(body)) {
            post.send(body);
        }

        if (ssl) {
            trustHttps(post);
        }

        return post.body();
    }

    private String doGet() {
        HttpRequest get = HttpRequest.get(url, params, encode)
                .headers(headers)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .uncompress(true);

        if (acceptGzip) {
            get.acceptGzipEncoding();
        }

        if (ssl) {
            trustHttps(get);
        }
        setOptionalHeaders(get);
        return get.body();
    }

    private void setOptionalHeaders(HttpRequest request) {
        if (!Strings.isNullOrEmpty(contentType)) {
            request.contentType(contentType, charset);
        }
        if (!Strings.isNullOrEmpty(accept)) {
            request.accept(accept);
        }
    }

    private void trustHttps(HttpRequest request) {
        request.trustAllCerts().trustAllHosts();
    }

    public static Http get(String url) {
        return new Http(url);
    }

    public static Http post(String url) {
        return new Http(url).method(HttpMethod.POST);
    }

    public static Http put(String url) {
        return new Http(url).method(HttpMethod.PUT);
    }

    public static Http delete(String url) {
        return new Http(url).method(HttpMethod.DELETE);
    }

    /**
     * upload file
     *
     * @param url       url
     * @param fieldName field name
     * @param file      file
     * @return string response
     */
    public static String upload(String url, String fieldName, File file) {
        try {
            return upload(url, fieldName, file.getName(), new FileInputStream(file));
        } catch (Exception e) {
            logger.error("failed to upload file(url={}, fieldName={}, file={}), cause: {}",
                    url, fieldName, file, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * upload file
     *
     * @param url       url
     * @param fieldName field name
     * @param in        InputStream
     * @return string response
     */
    public static String upload(String url, String fieldName, String fileName, InputStream in) {
        try {
            HttpRequest request = HttpRequest.post(url);
            request.part(fieldName, fileName, null, in);
            return request.body();
        } catch (Exception e) {
            logger.error("failed to upload file(url={}, fieldName={}, fileName), cause: {}",
                    url, fieldName, fileName, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * download a file
     *
     * @param url  http url
     * @param into the file which downloaded content will fill into
     */
    public static void download(String url, File into) {
        try {
            download(url, new FileOutputStream(into));
        } catch (FileNotFoundException e) {
            logger.error("failed to download ({}) into file({}), cause: {}",
                    url, into, Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * download a file
     *
     * @param url    http url
     * @param output the output which downloaded content will fill into
     */
    public static void download(String url, OutputStream output) {
        try {
            HttpRequest request = HttpRequest.get(url);
            if (request.ok()) {
                request.receive(output);
            } else {
                logger.warn("download request(url={}) isn't ok: {}, {}",
                        url, request.code(), request.body());
            }
        } catch (Exception e) {
            logger.error("failed to download file(url={}), cause: {}",
                    url, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * download a file
     *
     * @param url http url
     * @return the file content or null if request isn't ok
     */
    public static String download(String url) {
        try {
            HttpRequest request = HttpRequest.get(url);
            if (request.ok()) {
                File downloading = File.createTempFile("download", "");
                request.receive(downloading);
                return Files.toString(downloading, Charsets.UTF_8);
            } else {
                logger.warn("download request(url={}) isn't ok: {}, {}",
                        url, request.code(), request.body());
            }
            return null;
        } catch (Exception e) {
            logger.error("failed to download file(url={}), cause: {}",
                    url, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

    private static enum HttpMethod {
        GET, POST, PUT, DELETE
    }

}
