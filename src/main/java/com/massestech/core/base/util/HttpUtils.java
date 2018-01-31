package com.massestech.core.base.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by Administrator on 2015/8/13 0013.
 */
public class HttpUtils {
    /*logger*/
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    /*
     * 默认的String类型相应结果
     */
    private static final ResponseHandler<String> stringResponse = new ResponseHandler<String>() {
        @Override
        public String handleResponse(HttpResponse response)
                throws ClientProtocolException, IOException {
            //判断相应是否正确
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode>=200||statusCode<300){
                HttpEntity entity = response.getEntity();
                if(entity!=null){
                    return EntityUtils.toString(entity, "UTF-8");
                }
            }else{
                throw new HttpException("The response code is unexpected");
            }
            return null;
        }
    };

    /**
     * 获取HttpClient
     */
    private static final HttpClient getClient(){
        return HttpClients.createDefault();
    }

    /**
     * 获取requestUrl
     */
    public static final String getReqUrl(HttpParam param){
        if(param==null){
            //未传入请求配置
            throw new HttpException("MUST put HttpParam !!!");
        }
        String reqUrl = param.getUrl();
        //判断是否有参数
        if(param.isHaveParam()){
            //重新构造url
            StringBuffer sb = new StringBuffer();
            for(Entry<String,Object> entry : param.getParams().entrySet()){
                try {
                    sb.append(entry.getKey() + "=" + entry.getValue()+ "&");
                } catch (Exception e) {
                    //不会发生
                }
            }
            //去除最后一个“&”
            sb.delete(sb.lastIndexOf("&"), sb.length());
            //合并Url
            reqUrl += ((reqUrl.indexOf("?")<0 ? "?" : "&") + sb.toString());
        }

        logger.debug("get Url -> " + reqUrl);

        return reqUrl;
    }

    /**
     * 发送Get请求
     */
    public static final String get(String reqUrl){
        logger.debug("get Url -> " + reqUrl);
        try {
            return getClient().execute(new HttpGet(reqUrl), stringResponse);
        } catch (IOException e) {
            throw new HttpException("Url : " + reqUrl + " get Request ERROR", e);
        }
    }

    /**
     * 发送Get请求
     */
    public static final String get(HttpParam param){
        if(param==null){
            //未传入请求配置
            throw new HttpException("MUST put HttpParam !!!");
        }
        String reqUrl = param.getUrl();
        //判断是否有参数
        if(param.isHaveParam()){
            //重新构造url
            StringBuffer sb = new StringBuffer();
            for(Entry<String,Object> entry : param.getParams().entrySet()){
                try {
                    sb.append(entry.getKey() + "=" + URLEncoder.encode(
                            String.valueOf(entry.getValue()), "UTF-8") + "&");
                } catch (UnsupportedEncodingException e) {
                    //不会发生
                }
            }
            //去除最后一个“&”
            sb.delete(sb.lastIndexOf("&"), sb.length());
            //合并Url
            reqUrl += ((reqUrl.indexOf("?")<0 ? "?" : "&") + sb.toString());
        }
        logger.debug("get Url -> " + reqUrl);
        try {
            return getClient().execute(new HttpGet(reqUrl), stringResponse);
        } catch (IOException e) {
            logger.error("Url : " + param.getUrl() + " get Request ERROR", e);
            throw new HttpException("Url : " + param.getUrl() + " get Request ERROR", e);
        }
    }

    /**
     * 发送Get请求，自行处理返回响应
     */
    public static final HttpResponse getResponse(HttpParam param){
        if(param==null){
            //未传入请求配置
            throw new HttpException("MUST put HttpParam !!!");
        }
        String reqUrl = param.getUrl();
        //判断是否有参数
        if(param.isHaveParam()){
            //重新构造url
            StringBuffer sb = new StringBuffer();
            for(Entry<String,Object> entry : param.getParams().entrySet()){
                try {
                    sb.append(entry.getKey() + "=" + URLEncoder.encode(
                            String.valueOf(entry.getValue()), "UTF-8") + "&");
                } catch (UnsupportedEncodingException e) {
                    //不会发生
                }
            }
            //去除最后一个“&”
            sb.delete(sb.lastIndexOf("&"), sb.length());
            //合并Url
            reqUrl += ((reqUrl.indexOf("?")<0 ? "?" : "&") + sb.toString());
        }

        logger.debug("get Url -> " + reqUrl);

        try {
            return getClient().execute(new HttpGet(reqUrl));
        } catch (IOException e) {
            logger.error("Url : " + param.getUrl() + " getResponse ERROR!!!", e);
            throw new HttpException("Url : " + param.getUrl() + " getResponse Request ERROR!!!", e);
        }
    }

    /**
     * 发送Post请求
     */
    public static final String post(HttpParam param){
        if(param==null){
            //未传入请求配置
            throw new HttpException("MUST put HttpParam !!!");
        }
        HttpPost post = new HttpPost(param.getUrl());
        //是否需要设置参数
        if(param.isHaveParam()){
            try {
                post.setEntity(new UrlEncodedFormEntity(param.getNameValuePair(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                //不会发生
            }
        }
        logger.debug("post Url -> " + param.getUrl());
        try {
            return getClient().execute(post, stringResponse);
        } catch (Exception e) {
            logger.error("Url " + param.getUrl() + " post Request ERROR", e);
            throw new HttpException("Url " + param.getUrl() + " post Request ERROR");
        }
    }

    /**
     * 发送Post请求，自行处理返回响应
     */
    public static final HttpResponse postResponse(HttpParam param){
        if(param==null){
            //未传入请求配置
            throw new HttpException("MUST put HttpParam !!!");
        }
        HttpPost post = new HttpPost(param.getUrl());
        //是否需要设置参数
        if(param.isHaveParam()){
            try {
                post.setEntity(new UrlEncodedFormEntity(param.getNameValuePair(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                //不会发生
            }
        }
        logger.debug("post Url -> " + param.getUrl());
        try {
            return getClient().execute(post);
        } catch (Exception e) {
            logger.error("Url " + param.getUrl() + " post Request ERROR", e);
            throw new HttpException("Url " + param.getUrl() + " post Request ERROR");
        }
    }

    /**
     * Post文本
     * @param url 请求地址
     * @return
     */
    public static final InputStream post(String url, String content){
        if(StringUtils.isEmpty(url)){
            //未传入请求配置
            throw new HttpException("MUST put HttpParam !!!");
        }
        HttpPost post = new HttpPost(url);
        if(StringUtils.isNotEmpty(content)){
            logger.info("发送报文：" + content);
            post.setEntity(new StringEntity(content, "UTF-8"));
        }
        try {
            InputStream in =  getClient().execute(post).getEntity().getContent();
            return in;
        } catch (IOException e) {
            logger.error("Url " + url + " postContent Request ERROR", e);
            throw new HttpException("Url " + url + " postContent Request ERROR");
        }
    }

    /**
     * Post Json
     * @param url 请求地址
     * @param json 要post的文本
     * @return
     */
    public static final String postJson(String url, String json){
        if(StringUtils.isEmpty(url)){
            //未传入请求配置
            throw new HttpException("MUST put HttpParam !!!");
        }
        HttpPost post = new HttpPost(url);
        if(StringUtils.isNotEmpty(json)){
            logger.info("发送报文：" + json);
            post.addHeader("Content-type","application/json; charset=utf-8");
            post.setHeader("Accept", "application/json");
            post.setEntity(new StringEntity(json, "UTF-8"));
        }
        try {
            String result = getClient().execute(post, stringResponse);
            logger.info("返回报文：" + result);
            return result;
        } catch (IOException e) {
            logger.error("Url " + url + " postContent Request ERROR", e);
            throw new HttpException("Url " + url + " postContent Request ERROR");
        }
    }

    /**
     * 模拟form表单的形式 ，上传文件 以输出流的形式把文件写入到url中，然后用输入流来获取url的响应
     * @param url 请求地址 form表单url地址
     * @param file 文件
     * @return String url的响应信息返回值
     * @throws IOException
     */
    public static String postMedia(String url, File file) throws IOException {
        String result = null;
        if (!file.exists() || !file.isFile()) {
            logger.info("上传多媒体素材路径filePath: " + file.getName());
            throw new IOException("文件不存在");
        }
        /**
         * 第一部分
         */
        URL urlObj = new URL(url);
        // 连接
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
        /**
         * 设置关键值
         */
        con.setRequestMethod("POST"); // 以Post方式提交表单，默认get方式
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false); // post方式不能使用缓存
        // 设置请求头信息
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Charset", "UTF-8");
        // 设置边界
        String BOUNDARY = "----------" + System.currentTimeMillis();
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary="+ BOUNDARY);
        // 请求正文信息
        // 第一部分：
        StringBuilder sb = new StringBuilder();
        sb.append("--"); // 必须多两道线
        sb.append(BOUNDARY);
        sb.append("\r\n");
        sb.append("Content-Disposition: form-data;name=\"file\";filename=\""
                + file.getName() + "\"\r\n");
        sb.append("Content-Type:application/octet-stream\r\n\r\n");
        byte[] head = sb.toString().getBytes("utf-8");
        // 获得输出流
        OutputStream out = new DataOutputStream(con.getOutputStream());
        // 输出表头
        out.write(head);
        // 文件正文部分
        // 把文件已流文件的方式 推入到url中
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        int bytes = 0;
        byte[] bufferOut = new byte[1024];
        while ((bytes = in.read(bufferOut)) != -1) {
            out.write(bufferOut, 0, bytes);
        }
        in.close();
        // 结尾部分
        byte[] foot = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("utf-8");// 定义最后数据分隔线
        out.write(foot);
        out.flush();
        out.close();
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;
        try {
            // 定义BufferedReader输入流来读取URL的响应
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                buffer.append(line);
            }
            if(result==null){
                result = buffer.toString();
            }
        } catch (IOException e) {
            System.out.println("发送POST请求出现异常！" + e);
            e.printStackTrace();
            throw new IOException("数据读取异常");
        } finally {
            if(reader!=null){
                reader.close();
            }

        }
        return result;
    }



    /**
     * 用于封装Http请求参数
     */
    public static class HttpParam {

        private String url;
        private Map<String,Object> params = new HashMap<String, Object>();

        public HttpParam(String url) {
            this.url = url;
        }

        public HttpParam(String url, Map<String, Object> params) {
            this.url = url;
            this.params = params;
        }

        /**
         * 设置请求参数
         * @param name 参数名称
         * @param value 参数值
         * @return
         */
        public HttpParam addParam(String name, Object value){
            params.put(name, value);
            return this;
        }

        /**
         * 获取用于HttpClient访问的参数键值对
         * @return
         */
        public List<NameValuePair> getNameValuePair(){
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            if(params.size()>0){
                for(Entry<String,Object> entry : params.entrySet()){
                    nvps.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                }
            }
            return nvps;
        }

        /**
         * 判断是否已设置参数
         */
        public boolean isHaveParam(){
            return params.size()>0;
        }

        public String getUrl() {
            return url;
        }
        public HttpParam setUrl(String url) {
            this.url = url;
            return this;
        }
        public Map<String, Object> getParams() {
            return params;
        }
        public HttpParam setParams(Map<String, Object> params) {
            this.params = params;
            return this;
        }
    }

    /**
     * Http请求异常
     */
    public static class HttpException extends RuntimeException {

        private static final long serialVersionUID = 2042373458453875822L;

        public HttpException(String message, Throwable cause) {
            super(message, cause);
        }

        public HttpException(String message) {
            super(message);
        }

        public HttpException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * 获取参数
     * @return
     */
    public static String getRequestJson(HttpServletRequest request){
        InputStream is = null;
        BufferedReader br = null;
        StringBuffer sb = new StringBuffer();
        String line = null;
        try {
            //获取输入流
            is = request.getInputStream();
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while(StringUtils.isNotEmpty((line = br.readLine()))){
                sb.append(line);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(br!=null){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(is!=null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(sb.length()>0){
            return sb.toString();
        } else {
            return null;
        }
    }
}
