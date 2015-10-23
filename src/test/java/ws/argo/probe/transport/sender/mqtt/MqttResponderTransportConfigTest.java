package ws.argo.probe.transport.sender.mqtt;

import org.junit.Test;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.probe.transport.responder.mqtt.MqttResponderTransport;

/**
 * Created by jmsimpson on 10/21/15.
 */
public class MqttResponderTransportConfigTest {


    @Test
    public void testReadBasicConfig() throws TransportConfigException {
        MqttResponderTransport responderTransport = new MqttResponderTransport();

        responderTransport.initialize(null, "testMqttResponderConfig.xml");

    }

    @Test( expected = TransportConfigException.class)
    public void testBadConfigValues() throws TransportConfigException {
        MqttResponderTransport responderTransport = new MqttResponderTransport();

        responderTransport.initialize(null, "testBadBrokerMqttResponderConfig.xml");


    }

}
