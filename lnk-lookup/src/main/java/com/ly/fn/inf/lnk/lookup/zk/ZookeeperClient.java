package com.ly.fn.inf.lnk.lookup.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;
import java.util.List;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ly.fn.inf.lnk.lookup.zk.ChildListener;

/**
 * @author scott
 *
 */
public abstract class ZookeeperClient {
	protected static final Logger log = LoggerFactory.getLogger(ZookeeperClient.class);
	CuratorFramework client;
	private int connectionTimeoutMs;
	private int sessionTimeoutMs;
	private String connectServer;
	private HashSet<String> watchers = new HashSet<String>();
	private String namespace;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public ZookeeperClient(String connectServer) {
		this(connectServer, 30000, 30000, "/");
	}

	public ZookeeperClient(String connectServer, String namespace) {
		this(connectServer, 30000, 30000, namespace);
	}

	public ZookeeperClient(CuratorFramework client) {
		this.client = client;
		this.connectionTimeoutMs = client.getZookeeperClient().getConnectionTimeoutMs();
		this.connectServer = client.getZookeeperClient().getCurrentConnectionString();
		try {
			this.sessionTimeoutMs = client.getZookeeperClient().getZooKeeper().getSessionTimeout();
		} catch (Exception e) {
			this.sessionTimeoutMs = this.connectionTimeoutMs;
		}
	}

	public ZookeeperClient(String connectServer, int connectionTimeout, int sessionTimeout, String namespace) {
		this.connectServer = connectServer;
		this.connectionTimeoutMs = connectionTimeout;
		this.sessionTimeoutMs = sessionTimeout;
		this.namespace = namespace;
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 29);
		client = createWithOptions(connectServer, retryPolicy, connectionTimeout, sessionTimeout, namespace);
		client.start();
	}

	private CuratorFramework createWithOptions(String connectionString, RetryPolicy retryPolicy,
			int connectionTimeoutMs, int sessionTimeoutMs, String namespace) {
		return CuratorFrameworkFactory.builder().connectString(connectionString).namespace(namespace)
				.retryPolicy(retryPolicy).connectionTimeoutMs(connectionTimeoutMs).sessionTimeoutMs(sessionTimeoutMs)
				.build();
	}

	protected static CuratorFramework createSimple(String connectionString) {
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 29);
		return CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
	}

	protected boolean createTempPath(String path, String data) throws ZookeeperException {
		try {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data.getBytes());
		} catch (Exception e) {
			log.warn("createPath failed", e);
			throw new ZookeeperException("createPath failed");
		}
		return true;
	}

	protected boolean createPath(String path, String data) throws ZookeeperException {
		try {
			client.create().creatingParentsIfNeeded().forPath(path, data.getBytes());
		} catch (Exception e) {
			log.warn("createPath failed", e);
			throw new ZookeeperException("createPath failed");
		}
		return true;
	}

	protected List<String> getChildrens(String path) throws ZookeeperException {
		try {
			return client.getChildren().forPath(path);
		} catch (Exception e) {
			log.warn("getchildrens failed", e);
			throw new ZookeeperException("getchildrens failed");
		}
	}

	protected List<String> getChildrensWithWatcher(String path, ChildListener listener) throws ZookeeperException {
		try {
			ZKWatchChildren watcher = new ZKWatchChildren(listener);
			List<String> childrens = client.getChildren().usingWatcher(watcher).forPath(path);
			// 添加session过期的监控
			addReconnectionWatcher(path, ZookeeperWatcherType.GET_CHILDREN, watcher);
			return childrens;
		} catch (Exception e) {
			log.warn("getchildrens failed", e);
			throw new ZookeeperException("getchildrens failed");
		}
	}

	protected boolean checkPathExist(String path) throws ZookeeperException {
		try {
			return client.checkExists().forPath(path) != null;
		} catch (Exception e) {
			log.warn("checkPathExist failed", e);
			throw new ZookeeperException("checkPathExist failed");
		}
	}

	protected boolean checkPathExistWithWatcher(String path, final CuratorWatcher watcher) throws ZookeeperException {
		try {
			return client.checkExists().usingWatcher(watcher).forPath(path) != null;
		} catch (Exception e) {
			log.warn("checkPathExist failed", e);
			throw new ZookeeperException("checkPathExist failed");
		}
	}

	protected boolean setData(String path, String payload) throws ZookeeperException {
		try {
			client.setData().forPath(path, payload.getBytes());
			return true;
		} catch (Exception e) {
			log.warn("updateData failed", e);
			throw new ZookeeperException("updateData failed");
		}
	}

	protected boolean deletePath(String path) throws ZookeeperException {
		try {
			client.delete().forPath(path);
			return true;
		} catch (Exception e) {
			log.warn("deletePath failed", e);
			throw new ZookeeperException("deletePath failed");
		}
	}

	protected boolean deletePathTree(String path) throws ZookeeperException {
		try {
			client.delete().deletingChildrenIfNeeded().forPath(path);
			return true;
		} catch (Exception e) {
			log.warn("deletePathTree failed", e);
			throw new ZookeeperException("deletePathTree failed");
		}
	}

	protected boolean guaranteedDeletePathTree(String path) throws ZookeeperException {
		try {
			client.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
			return true;
		} catch (Exception e) {
			log.warn("deletePathTree failed", e);
			throw new ZookeeperException("deletePathTree failed");
		}
	}

	protected String getData(String path) throws ZookeeperException {
		try {
			byte[] buffer = client.getData().forPath(path);
			return new String(buffer, "UTF-8");
		} catch (Exception e) {
			log.warn("getData failed", e);
			throw new ZookeeperException("getData failed");
		}
	}

	protected String getDataWithWatcher(String path, final CuratorWatcher watcher) throws ZookeeperException {
		try {
			byte[] buffer = client.getData().usingWatcher(watcher).forPath(path);
			// 添加session过期的监控
			addReconnectionWatcher(path, ZookeeperWatcherType.GET_DATA, watcher);
			return new String(buffer, "UTF-8");
		} catch (Exception e) {
			log.warn("getDataWithWatcher failed", e);
			throw new ZookeeperException("getDataWithWatcher failed");
		}
	}

	private void addReconnectionWatcher(final String path, final ZookeeperWatcherType watcherType,
			final CuratorWatcher watcher) {
		synchronized (this) {
			if (!watchers.contains(watcher.toString())) // 不要添加重复的监听事件
			{
				watchers.add(watcher.toString());
				log.info("add new watcher " + watcher);
				client.getConnectionStateListenable().addListener(new ConnectionStateListener() {

					public void stateChanged(CuratorFramework client, ConnectionState newState) {
						if (newState == ConnectionState.RECONNECTED) {// 处理session过期
							int tryCount = 0;
							while (tryCount++ < 3) {
								try {
									// if
									// (client.getZookeeperClient().blockUntilConnectedOrTimedOut())
									// {
									if (watcherType == ZookeeperWatcherType.EXITS) {
										log.info("EXITS recover path :" + path + ", watch:" + watcher.toString());
										client.checkExists().usingWatcher(watcher).forPath(path);
									} else if (watcherType == ZookeeperWatcherType.GET_CHILDREN) {
										log.info(
												"GET_CHILDREN recover path :" + path + ", watch:" + watcher.toString());
										client.getChildren().usingWatcher(watcher).forPath(path);
									} else if (watcherType == ZookeeperWatcherType.GET_DATA) {
										log.info("GET_DATA recover path :" + path + ", watch:" + watcher.toString());
										client.checkExists().usingWatcher(watcher).forPath(path);
									} else if (watcherType == ZookeeperWatcherType.CREATE_ON_NO_EXITS) {
										// ephemeral类型的节点session过期了，需要重新创建节点，并且注册监听事件，之后监听事件中，
										// 会处理create事件，将路径值恢复到先前状态
										log.info("CREATE_ON_NO_EXITS recover path :" + path + ", watch:"
												+ watcher.toString());
										if (ZookeeperClient.this.isServiceActive(path)) {
											Stat stat = client.checkExists().usingWatcher(watcher).forPath(path);
											if (stat == null) {
												log.info("create new zookeeper node : " + path);
												client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
														.withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath(path);
											}
										}
									}
									break;
									// }
								} catch (InterruptedException e) {
									log.warn(
											"path:" + path
													+ ", zookeeper session timeout stateChanged handle InterruptedException:",
											e);
									break;
								} catch (Exception e) {
									log.warn("path:" + path
											+ ", zookeeper session timeout stateChanged handle exception:", e);
								}
							}
						}
					}
				});
			}
		}
	}

	public String getServer() {
		return this.connectServer;
	}

	public int getConnectionTimeout() {
		return this.connectionTimeoutMs;
	}

	public int getSessionTimeout() {
		return this.sessionTimeoutMs;
	}

	protected void register(String path, String data) throws Exception {

		CuratorWatcher watcher = new ZKWatchRegister(path, data); // 创建一个register
																	// watcher

		Stat stat = client.checkExists().forPath(path);
		if (stat != null) {
			client.delete().deletingChildrenIfNeeded().forPath(path);
		}
		client.checkExists().usingWatcher(watcher).forPath(path);
		client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
				.forPath(path, data.getBytes());// 创建的路径和值

		// 添加到session过期监控事件中
		addReconnectionWatcher(path, ZookeeperWatcherType.CREATE_ON_NO_EXITS, watcher);
	}

	public abstract boolean isServiceActive(String path);

	private class ZKWatchRegister implements CuratorWatcher {
		private final String path;
		private byte[] value;

		@SuppressWarnings("unused")
		public String getPath() {
			return path;
		}

		public ZKWatchRegister(String path, String value) {
			this.path = path;
			this.value = value.getBytes();
		}

		public void process(WatchedEvent event) throws Exception {
			// System.out.println(event.getType());
			if (event.getType() == EventType.NodeDataChanged) {
				// 节点数据改变了，需要记录下来，以便session过期后，能够恢复到先前的数据状态
				byte[] data = client.getData().usingWatcher(this).forPath(path);
				value = data;
			} else if (event.getType() == EventType.NodeDeleted) {
				// 节点被删除了，需要创建新的节点
				log.info("path:" + event.getPath() + " has been deleted.");
				if (ZookeeperClient.this.isServiceActive(event.getPath())) {
					Stat stat = client.checkExists().usingWatcher(this).forPath(path);
					if (stat == null) {
						client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
								.withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath(path);
					}
				}
			} else if (event.getType() == EventType.NodeCreated) {
				// 节点被创建时，需要添加监听事件（创建可能是由于session过期后，curator的状态监听部分触发的）
				log.info("paht:" + path + " has been created!" + "the current data is " + new String(value));
				client.setData().forPath(path, value);
				client.checkExists().usingWatcher(this).forPath(path);
			}
		}
	}

	private class ZKWatchChildren implements CuratorWatcher {

		private volatile ChildListener listener;

		public ZKWatchChildren(ChildListener listener) {
			this.listener = listener;
		}

		public void process(WatchedEvent event) throws Exception {
			if (event.getType() == EventType.NodeChildrenChanged) {
				if (!ZookeeperClient.this.checkPathExist(event.getPath())) {
					ZookeeperClient.this.createPath(event.getPath(), "");
				}
				// 节点变动，获取节点信息，重新注册监听
				List<String> childrens = client.getChildren().usingWatcher(this).forPath(event.getPath());
				// 更新内存信息
				if (listener != null) {
					listener.childChanged(event.getPath(), childrens);
				}
			} else if (event.getType() == EventType.None && event.getPath() == null) {
				log.info("ZKWatchChildren zookeeper session timeout");
			} else {
				client.checkExists().usingWatcher(this).forPath(event.getPath());
			}
		}
	}

	protected String createPath(String serviceGroup, String serviceId, int version, int protocol) {
		StringBuilder sb = new StringBuilder("/");
		sb.append(serviceGroup).append("/").append(serviceId).append("/").append(version).append("/").append(protocol)
				.append("/server");
		return sb.toString();
	}

	protected enum ZookeeperWatcherType {
		GET_DATA, GET_CHILDREN, EXITS, CREATE_ON_NO_EXITS
	}
}
