package ws.argo.probe.transport.responder.mqtt;

import ws.argo.plugin.transport.responder.Transport;
import ws.argo.plugin.transport.responder.TransportConfigException;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;

import java.util.Properties;

/**
 * Created by jmsimpson on 10/19/15.
 */
public class MqttResponderTransport implements Transport{
    public void initialize(Properties properties, String s) throws TransportConfigException {

    }

    public void sendProbe(Probe probe) throws ProbeSenderException {

    }

    public int maxPayloadSize() {
        return 0;
    }

    public String getNetworkInterfaceName() {
        return null;
    }

    public void close() throws ProbeSenderException {

    }
}
