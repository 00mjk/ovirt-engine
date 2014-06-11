package org.ovirt.engine.core.vdsbroker.jsonrpc;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ovirt.vdsm.jsonrpc.client.ClientConnectionException;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcClient;
import org.ovirt.vdsm.jsonrpc.client.internal.ResponseWorker;
import org.ovirt.vdsm.jsonrpc.client.reactors.ManagerProvider;
import org.ovirt.vdsm.jsonrpc.client.reactors.Reactor;
import org.ovirt.vdsm.jsonrpc.client.reactors.ReactorClient;
import org.ovirt.vdsm.jsonrpc.client.reactors.ReactorFactory;
import org.ovirt.vdsm.jsonrpc.client.reactors.ReactorType;
import org.ovirt.vdsm.jsonrpc.client.utils.retry.RetryPolicy;

public class JsonRpcUtils {
    private static Log log = LogFactory.getLog(JsonRpcUtils.class);

    public static JsonRpcClient createStompClient(String hostname, int port, int connectionTimeout,
            int clientTimeout, int connectionRetry, boolean isSecure) {
        return createClient(hostname, port, connectionTimeout, clientTimeout, connectionRetry, isSecure, ReactorType.STOMP);
    }

    private static ManagerProvider getManagerProvider(boolean isSecure) {
        ManagerProvider provider = null;
        if (isSecure) {
            provider = new EngineManagerProvider();
        }
        return provider;
    }

    private static JsonRpcClient createClient(String hostname, int port, int connectionTimeout,
            int clientTimeout, int connectionRetry, boolean isSecure, ReactorType type) {
        final ManagerProvider provider = getManagerProvider(isSecure);
        try {
            final Reactor reactor = ReactorFactory.getReactor(provider, type);
            return getJsonClient(reactor, hostname, port, connectionTimeout, clientTimeout, connectionRetry);
        } catch (ClientConnectionException e) {
            log.error("Exception occured during building ssl context or obtaining selector", e);
            throw new IllegalStateException(e);
        }
    }

    private static JsonRpcClient getJsonClient(Reactor reactor, String hostName, int port, int connectionTimeOut,
            int clientTimeOut, int connectionRetry) throws ClientConnectionException {
        final ReactorClient client = reactor.createClient(hostName, port);
        client.setRetryPolicy(new RetryPolicy(connectionTimeOut, connectionRetry, IOException.class));
        ResponseWorker worker = ReactorFactory.getWorker();
        JsonRpcClient jsonClient = worker.register(client);
        jsonClient.setRetryPolicy(new RetryPolicy(clientTimeOut, connectionRetry, IOException.class));
        return jsonClient;
    }
}
