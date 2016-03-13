/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.rsf.center.core.zookeeper.node;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.hasor.core.AppContext;
import net.hasor.rsf.center.core.zookeeper.ZkNodeType;
import net.hasor.rsf.center.core.zookeeper.ZooKeeperNode;
import net.hasor.rsf.center.domain.constant.RsfCenterCfg;
import net.hasor.rsf.center.domain.constant.RsfCenterEvent;
/**
 * 集群客户端模式，加入已有ZK集群。作为ZK客户端还提供了对ZK的读写功能。
 * @version : 2015年8月19日
 * @author 赵永春(zyc@hasor.net)
 */
public class ZooKeeperNode_Slave implements ZooKeeperNode, Watcher {
    protected Logger     logger     = LoggerFactory.getLogger(getClass());
    private AppContext   appContext = null;
    private String       serverConnection;
    private RsfCenterCfg zooKeeperCfg;
    private ZooKeeper    zooKeeper;
    //
    public ZooKeeperNode_Slave(RsfCenterCfg zooKeeperCfg) {
        this.zooKeeperCfg = zooKeeperCfg;
    }
    //
    public void process(WatchedEvent event) {
        if (KeeperState.SyncConnected == event.getState()) {
            logger.info("zookeeper client -> SyncConnected.");// 链接成功。
            appContext.getEnvironment().getEventContext().fireSyncEvent(RsfCenterEvent.SyncConnected_Event, this);
        }
    }
    //
    /** 终止ZooKeeper */
    public void shutdownZooKeeper(AppContext appContext) throws IOException, InterruptedException {
        if (zooKeeper != null) {
            zooKeeper.close();
            zooKeeper = null;
        }
    }
    /** 启动ZooKeeper */
    public void startZooKeeper(AppContext appContext) throws IOException, InterruptedException {
        this.startZooKeeper(appContext, zooKeeperCfg.getZkServersStr());
    }
    /** 启动ZooKeeper */
    protected void startZooKeeper(AppContext appContext, String serverConnection) throws IOException, InterruptedException {
        logger.info("zkClient connected to {}.", serverConnection);
        this.appContext = appContext;
        this.serverConnection = serverConnection;
        this.zooKeeper = new ZooKeeper(this.serverConnection, zooKeeperCfg.getClientTimeout(), this);
        logger.info("zkClient connected -> ok.");
    }
    //
    //
    @Override
    public ZooKeeper getZooKeeper() {
        return this.zooKeeper;
    }
    @Override
    public void createNode(ZkNodeType nodtType, String nodePath) throws KeeperException, InterruptedException {
        if (this.zooKeeper.exists(nodePath, false) == null) {
            try {
                this.zooKeeper.delete(nodePath, -1);
            } catch (NoNodeException e) {
                // e: NoNodeException => // do nothing
            }
            try {
                String parent = new File(nodePath).getParent();
                if (this.zooKeeper.exists(parent, false) == null) {
                    this.createNode(nodtType, parent);
                }
                String result = this.zooKeeper.create(nodePath, null, Ids.OPEN_ACL_UNSAFE, nodtType.getNodeType());
                logger.debug("zkClient createNode {} -> {}", nodePath, result);
            } catch (NodeExistsException e) {
                logger.warn("zkClient createNode {} -> NodeExistsException ,maybe someone created first.-> {}", nodePath, e.getMessage());
            } catch (NoNodeException e) {
                logger.warn("zkClient createNode {} -> NoNodeException -> {}", nodePath, e.getMessage());
            }
        } else {
            logger.info("zkClient createNode {} -> exists.", nodePath);
        }
    }
    @Override
    public void deleteNode(String nodePath) throws KeeperException, InterruptedException {
        if (this.zooKeeper.exists(nodePath, false) != null) {
            try {
                List<String> childrenList = this.zooKeeper.getChildren(nodePath, false);
                if (childrenList != null) {
                    for (String itemNodePath : childrenList) {
                        this.deleteNode(nodePath + "/" + itemNodePath);
                    }
                }
                this.zooKeeper.delete(nodePath, -1);
                logger.debug("zkClient deleteNode {}", nodePath);
            } catch (NoNodeException e) {
                logger.warn("zkClient deleteNode {} -> NoNodeException ,maybe someone deleted first.-> {}", nodePath, e.getMessage());
            }
        } else {
            logger.info("zkClient deleteNode {} -> is not exists.", nodePath);
        }
    }
    @Override
    public Stat saveOrUpdate(ZkNodeType nodtType, String nodePath, String data) throws KeeperException, InterruptedException {
        Stat stat = this.zooKeeper.exists(nodePath, false);
        if (stat == null) {
            this.createNode(nodtType, nodePath);
            stat = this.zooKeeper.exists(nodePath, false);
            if (stat == null) {
                return null;
            }
        }
        //
        byte[] byteDatas = (data == null) ? null : data.getBytes();
        stat = this.zooKeeper.setData(nodePath, byteDatas, stat.getVersion());
        logger.debug("zkClient saveOrUpdate Node {}", nodePath);
        return stat;
    }
    @Override
    public String readData(String nodePath) throws KeeperException, InterruptedException {
        Stat stat = this.zooKeeper.exists(nodePath, false);
        if (stat == null) {
            return null;
        }
        //
        byte[] byteDatas = this.zooKeeper.getData(nodePath, false, stat);
        logger.debug("zkClient readData Node {}", nodePath);
        return new String(byteDatas);
    }
}