package com.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CuratorTest {

    // Curator 客户端对象
    private CuratorFramework client;
    // client2 用户模拟其他客户端
    private CuratorFramework client2;

    private CuratorFramework client3;

    // 初始化资源
    @Before
    public void init() throws Exception {
        // ZooKeeper 服务地址, 单机格式为:(127.0.0.1:2181), 集群格式为:(127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183)
        String connectString = "192.168.99.100:2181";
        // 重试策略 初始休眠时间为 1000ms, 最大重试次数为 3
        RetryPolicy retry = new ExponentialBackoffRetry(1000, 3);

        // 创建一个客户端, 60000(ms)为 session 超时时间, 15000(ms)为链接超时时间
        //connectionString      服务器列表，格式host1:port1,host2:port2,...
        // sessionTimeoutMs     会话超时时间，单位毫秒，默认60000ms
        //connectionTimeoutMs   连接创建超时时间，单位毫秒，默认60000ms
        //retryPolicy           重试策略,内建有四种重试策略,也可以自行实现RetryPolicy接口
        client = CuratorFrameworkFactory.newClient(connectString, 60000, 15000, retry);
        client2 = CuratorFrameworkFactory.newClient(connectString, 60000, 15000, retry);

        //Fluent风格的Api创建会话
        client3 =
                CuratorFrameworkFactory.builder()
                        .connectString("192.168.99.100:2181")
                        .sessionTimeoutMs(5000)
                        .connectionTimeoutMs(5000)
                        .retryPolicy(retry)
                        //为了实现不同的Zookeeper业务之间的隔离，需要为每个业务分配一个独立的命名空间（NameSpace），
                        // 即指定一个Zookeeper的根路径（官方术语：为Zookeeper添加“Chroot”特性）。
                        // 例如（下面的例子）当客户端指定了独立命名空间为“/base”，
                        // 那么该客户端对Zookeeper上的数据节点的操作都是基于该目录进行的。
                        // 通过设置Chroot可以将客户端应用与Zookeeper服务端的一课子树相对应，
                        // 在多个应用共用一个Zookeeper集群的场景下，这对于实现不同应用之间的相互隔离十分有意义
                        .namespace("base")
                        .build();


        // 创建会话
        client.start();
        client2.start();
        CuratorFrameworkState state = client.getState();
        System.out.println("当前客户端1的状态：" + ( CuratorFrameworkState.STARTED == state ? "连接中..." : "已关闭..." ));
    }

    /**
     * 基本API
     */
    @Test
    public void function() throws Exception {
        // 创建节点
        String nodePath = "/super/testNode";  // 节点路径
        byte[] data = "this is a test data".getBytes();  // 节点数据
        String result = client.create().creatingParentsIfNeeded()  // 创建父节点，也就是会递归创建
                .withMode(CreateMode.PERSISTENT)  // 节点类型
                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)  // 节点的acl权限
                .forPath(nodePath, data);

        System.out.println(result + "节点，创建成功...");


        // 添加 watcher 事件，当使用usingWatcher的时候，监听只会触发一次，监听完毕后就销毁
        client.getData().usingWatcher(new MyCuratorWatcher()).forPath(nodePath);
        // curatorConnect.client.getData().usingWatcher(new MyWatcher()).forPath(nodePath);

        //*******************************curator之nodeCache一次注册N次监听**************************************************
        // NodeCache: 缓存节点，并且可以监听数据节点的变更，会触发事件
        final NodeCache nodeCache = new NodeCache(client, nodePath);

        // 参数 buildInitial : 初始化的时候获取node的值并且缓存
        nodeCache.start(true);

        // 获取缓存里的节点初始化数据
        if (nodeCache.getCurrentData() != null) {
            System.out.println("节点初始化数据为：" + new String(nodeCache.getCurrentData().getData()));
        } else {
            System.out.println("节点初始化数据为空...");
        }

        // 为缓存的节点添加watcher，或者说添加监听器
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            // 节点数据change事件的通知方法
            public void nodeChanged() throws Exception {
                // 防止节点被删除时发生错误
                if (nodeCache.getCurrentData() == null) {
                    System.out.println("获取节点数据异常，无法获取当前缓存的节点数据，可能该节点已被删除");
                    return;
                }
                // 获取节点最新的数据
                String data = new String(nodeCache.getCurrentData().getData());
                System.out.println(nodeCache.getCurrentData().getPath() + " 节点的数据发生变化，最新的数据为：" + data);
            }
        });
        //*********************************************************************************
        // 为子节点添加watcher
        // PathChildrenCache: 监听数据节点的增删改，可以设置触发的事件
        final PathChildrenCache childrenCache = new PathChildrenCache(client, nodePath, true);

        /**
         * StartMode: 初始化方式
         * POST_INITIALIZED_EVENT：异步初始化，初始化之后会触发事件
         * NORMAL：异步初始化
         * BUILD_INITIAL_CACHE：同步初始化
         */
        childrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        //异步
//        childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

        // 列出子节点数据列表，需要使用BUILD_INITIAL_CACHE同步初始化模式才能获得，异步是获取不到的
        List<ChildData> childDataList = childrenCache.getCurrentData();
        System.out.println("当前节点的子节点详细数据列表：");
        for (ChildData childData : childDataList) {
            System.out.println("\t* 子节点路径：" + childData.getPath() + "，该节点的数据为：" + new String(childData.getData()));
        }

        // 添加事件监听器
        childrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
                // 通过判断event type的方式来实现不同事件的触发
                if (event.getType().equals(PathChildrenCacheEvent.Type.INITIALIZED)) {  // 子节点初始化时触发
                    System.out.println("\n--------------\n");
                    System.out.println("子节点初始化成功");
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {  // 添加子节点时触发
                    System.out.println("\n--------------\n");
                    System.out.print("子节点：" + event.getData().getPath() + " 添加成功，");
                    System.out.println("该子节点的数据为：" + new String(event.getData().getData()));
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {  // 删除子节点时触发
                    System.out.println("\n--------------\n");
                    System.out.println("子节点：" + event.getData().getPath() + " 删除成功");
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {  // 修改子节点数据时触发
                    System.out.println("\n--------------\n");
                    System.out.print("子节点：" + event.getData().getPath() + " 数据更新成功，");
                    System.out.println("子节点：" + event.getData().getPath() + " 新的数据为：" + new String(event.getData().getData()));
                }
            }
        });

        //*********************************************************************************

        // 更新节点数据
        byte[] newData = "this is a new data".getBytes();
        Stat resultStat = client.setData().withVersion(0)  // 指定数据版本
                .forPath(nodePath, newData);  // 需要修改的节点路径以及新数据

        System.out.println("更新节点数据成功，新的数据版本为：" + resultStat.getVersion());

        // 获取子节点列表
        List<String> childNodes = client.getChildren().forPath(nodePath);
        System.out.println(nodePath + " 节点下的子节点列表：");
        for (String childNode : childNodes) {
            System.out.println(childNode);
        }

        // 查询某个节点是否存在，存在就会返回该节点的状态信息，如果不存在的话则返回空
        Stat statExist = client.checkExists().forPath(nodePath);
        if (statExist == null) {
            System.out.println(nodePath + " 节点不存在");
        } else {
            System.out.println(nodePath + " 节点存在");
        }


        // 删除节点
        client.delete()
                .guaranteed()  // 如果删除失败，那么在后端还是会继续删除，直到成功
                .deletingChildrenIfNeeded()  // 子节点也一并删除，也就是会递归删除
                .withVersion(resultStat.getVersion())
                .forPath(nodePath);
    }

    /**
     * 分布式锁
     */
    @Test
    public void sharedLock() throws Exception {
        // 创建共享锁
        // ZooKeeper 锁节点路径, 分布式锁的相关操作都是在这个节点上进行
        String lockPath = "/distributed-lock";
        InterProcessLock lock = new InterProcessSemaphoreMutex(client, lockPath);
        // lock2 用于模拟其他客户端
        InterProcessLock lock2 = new InterProcessSemaphoreMutex(client2, lockPath);

        // 获取锁对象
        lock.acquire();

        // 测试是否可以重入
        // 超时获取锁对象(第一个参数为时间, 第二个参数为时间单位), 因为锁已经被获取, 所以返回 false
        Assert.assertFalse(lock.acquire(2, TimeUnit.SECONDS));
        // 释放锁
        lock.release();

        // lock2 尝试获取锁成功, 因为锁已经被释放
        Assert.assertTrue(lock2.acquire(2, TimeUnit.SECONDS));
        lock2.release();
    }


    // 释放资源
    @After
    public void close() {
        CloseableUtils.closeQuietly(client);
        CloseableUtils.closeQuietly(client2);
        CloseableUtils.closeQuietly(client3);
        CuratorFrameworkState state = client.getState();
        System.out.println("当前客户端1的状态：" + ( CuratorFrameworkState.STARTED == state ? "连接中..." : "已关闭..." ));
    }


    private class MyWatcher implements Watcher {

        // Watcher事件通知方法
        @Override
        public void process(WatchedEvent watchedEvent) {
            System.out.println("触发watcher，节点路径为：" + watchedEvent.getPath());
        }
    }

    private class MyCuratorWatcher implements CuratorWatcher {

        // Watcher事件通知方法
        public void process(WatchedEvent watchedEvent) throws Exception {
            System.out.println("触发watcher，节点路径为：" + watchedEvent.getPath());
        }
    }

}


