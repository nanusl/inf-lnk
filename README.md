# inf-lnk

Lnk基于Netty实现RPC通讯协议，支持同步，异步和异步回调三种调用方式。支持多种负载均衡方式，使用信号量做流量控制，支持zookeeper和consul等服务注册发现方式，服务端口支持开发人员，运维人员配置以及动态分配，支持服务依赖关系梳理以及调用链路跟踪。支持spring配置。在服务端通过分组策略将来自不同组别的请求处理资源隔离，该思路借鉴与RocketMQ的实现思想。

# 使用Spring进行配置

	<inf-lnk:server id="paymentServer" listen-port="8888" server-worker-threads="20" server-selector-threads="15" 
		server-channel-maxidletime-seconds="120" server-socket-sndbuf-size="65535" server-socket-rcvbuf-size="65535" 
		server-pooled-bytebuf-allocator-enable="true" default-server-worker-processor-threads="10" default-server-executor-threads="8" 
		use-epoll-native-selector="false">
		<inf-lnk:application app="biz-pay-bgw-payment-srv" type="jar"/>
		<inf-lnk:registry type="zk" address="127.0.0.1:2181"/>
		<inf-lnk:flow-control type="semaphore" permits="10000"/>
		<inf-lnk:track type="logger"/>
		<inf-lnk:bind>
			<inf-lnk:service-group service-group="biz-pay-bgw-payment.srv" service-group-worker-processor-threads="30"/>
			<inf-lnk:service-group service-group="biz-pay-bgw-payment.router.srv" service-group-worker-processor-threads="30"/>
		</inf-lnk:bind>
	</inf-lnk:server>
	
	<inf-lnk:client id="paymentClient" client-worker-threads="4" connect-timeout-millis="3000" client-channel-maxidletime-seconds="120"
		client-socket-sndbuf-size="65535" client-socket-rcvbuf-size="65535" default-client-executor-threads="4">
		<inf-lnk:application app="biz-pay-bgw-payment-srv" type="jar"/>
		<inf-lnk:lookup type="zk" address="127.0.0.1:2181"/>
		<inf-lnk:flow-control type="semaphore" permits="1000"/>
		<inf-lnk:load-balance type="hash"/>
	</inf-lnk:client>
	
# Java代码中注解配置

	
	@LnkService(group = "biz-pay-bgw-payment.srv")
	public interface HelloService {

    		@LnkMethod(type = InvokeType.SYNC)
    		String welcome1(String name);

    		@LnkMethod(type = InvokeType.ASYNC)
    		void welcome2(String name, CommandCallback<String> callback);
	}
	
# 服务端依赖注入

	@Lnkwired(localWiredPriority = false)
     protected HelloService helloService;
    
     @Lnkwired(localWiredPriority = false)
     protected DemoService demoService;
    
# 简单Spring配置

	<inf-lnk:server id="paymentServer" >
		<inf-lnk:application app="biz-pay-bgw-payment-srv"/>
		<inf-lnk:registry type="zk" address="127.0.0.1:2181"/>
		<inf-lnk:flow-control type="semaphore" permits="10000000"/>
		<inf-lnk:track type="logger"/>
		<inf-lnk:bind>
			<inf-lnk:service-group service-group="biz-pay-bgw-payment.srv" service-group-worker-processor-threads="10"/>
			<inf-lnk:service-group service-group="biz-pay-bgw-payment.router.srv" service-group-worker-processor-threads="10"/>
		</inf-lnk:bind>
	</inf-lnk:server>
	
	<inf-lnk:client>
		<inf-lnk:application app="biz-pay-bgw-payment-srv"/>
		<inf-lnk:lookup type="zk" address="127.0.0.1:2181"/>
		<inf-lnk:flow-control type="semaphore" permits="1000"/>
		<inf-lnk:load-balance type="hash"/>
	</inf-lnk:client>
     
# 极简Spring配置

	<inf-lnk:server id="paymentServer" >
		<inf-lnk:application app="biz-pay-bgw-payment-srv"/>
		<inf-lnk:registry type="zk" address="127.0.0.1:2181"/>
	</inf-lnk:server>
	
	<inf-lnk:client>
		<inf-lnk:application app="biz-pay-bgw-payment-srv"/>
		<inf-lnk:lookup type="zk" address="127.0.0.1:2181"/>
		<inf-lnk:load-balance type="hash"/>
	</inf-lnk:client>
	
	
	






