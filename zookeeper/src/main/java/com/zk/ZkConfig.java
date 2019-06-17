package com.zk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 分布式配置
 */
public class ZkConfig {
    public CuratorFramework client = null;
    public static final String zkServerIp = "192.168.99.100:2181";

    // 初始化重连策略以及客户端对象并启动
    public ZkConfig() {
        RetryPolicy retryPolicy = new RetryNTimes(3, 5000);
        client = CuratorFrameworkFactory.builder()
                .connectString(zkServerIp)
                .sessionTimeoutMs(10000).retryPolicy(retryPolicy)
                .namespace("workspace").build();
        client.start();
    }

    // 关闭客户端
    public void closeZKClient() {
        if (client != null) {
            this.client.close();
        }
    }

    //  public final static String CONFIG_NODE = "/super/testNode/redis-config";
    public final static String CONFIG_NODE_PATH = "/super/testNode";
    public final static String SUB_PATH = "/redis-config";
    public static CountDownLatch countDown = new CountDownLatch(1);  // 计数器

    public static void main(String[] args) throws Exception {
        ZkConfig cto = new ZkConfig();
        System.out.println("client1 启动成功...");

        // 开启子节点缓存
        final PathChildrenCache childrenCache = new PathChildrenCache(cto.client, CONFIG_NODE_PATH, true);
        childrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        // 添加子节点监听事件
        childrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                // 监听节点的数据更新事件
                if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
                    String configNodePath = event.getData().getPath();
                    if (configNodePath.equals(CONFIG_NODE_PATH + SUB_PATH)) {
                        System.out.println("监听到配置发生变化，节点路径为:" + configNodePath);

                        // 读取节点数据
                        String jsonConfig = new String(event.getData().getData());
                        System.out.println("节点" + CONFIG_NODE_PATH + "的数据为: " + jsonConfig);

                        // 从json转换配置
                        RedisConfig redisConfig = null;
                        if (StringUtils.isNotBlank(jsonConfig)) {
                            redisConfig = JsonUtils.jsonToPojo(jsonConfig, RedisConfig.class);
                        }

                        // 配置不为空则进行相应操作
                        if (redisConfig != null) {
                            String type = redisConfig.getType();
                            String url = redisConfig.getUrl();
                            String remark = redisConfig.getRemark();
                            // 判断事件
                            if (type.equals("add")) {
                                System.out.println("\n-------------------\n");
                                System.out.println("监听到新增的配置，准备下载...");
                                // ... 连接ftp服务器，根据url找到相应的配置
                                Thread.sleep(500);
                                System.out.println("开始下载新的配置文件，下载路径为<" + url + ">");
                                // ... 下载配置到你指定的目录
                                Thread.sleep(1000);
                                System.out.println("下载成功，已经添加到项目中");
                                // ... 拷贝文件到项目目录
                            } else if (type.equals("update")) {
                                System.out.println("\n-------------------\n");
                                System.out.println("监听到更新的配置，准备下载...");
                                // ... 连接ftp服务器，根据url找到相应的配置
                                Thread.sleep(500);
                                System.out.println("开始下载配置文件，下载路径为<" + url + ">");
                                // ... 下载配置到你指定的目录
                                Thread.sleep(1000);
                                System.out.println("下载成功...");
                                System.out.println("删除项目中原配置文件...");
                                Thread.sleep(100);
                                // ... 删除原文件
                                System.out.println("拷贝配置文件到项目目录...");
                                // ... 拷贝文件到项目目录
                            } else if (type.equals("delete")) {
                                System.out.println("\n-------------------\n");
                                System.out.println("监听到需要删除配置");
                                System.out.println("删除项目中原配置文件...");
                            }
                            // TODO 视情况统一重启服务
                        }
                    }
                }
            }
        });

        countDown.await();

        cto.closeZKClient();
    }

    private static class JsonUtils {

        // 定义jackson对象
        private static final ObjectMapper MAPPER = new ObjectMapper();

        /**
         * 将对象转换成json字符串。
         * <p>Title: pojoToJson</p>
         * <p>Description: </p>
         *
         * @param data
         * @return
         */
        public static String objectToJson(Object data) {
            try {
                String string = MAPPER.writeValueAsString(data);
                return string;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 将json结果集转化为对象
         *
         * @param jsonData json数据
         * @param beanType 对象中的object类型
         * @return
         */
        public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
            try {
                T t = MAPPER.readValue(jsonData, beanType);
                return t;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 将json数据转换成pojo对象list
         * <p>Title: jsonToList</p>
         * <p>Description: </p>
         *
         * @param jsonData
         * @param beanType
         * @return
         */
        public static <T> List<T> jsonToList(String jsonData, Class<T> beanType) {
            JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
            try {
                List<T> list = MAPPER.readValue(jsonData, javaType);
                return list;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class RedisConfig implements Serializable {

        private String type;    // add 新增配置 update 更新配置 delete 删除配置
        private String url;        // 如果是add或update，则提供下载地址
        private String remark;    // 备注

        String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

}
