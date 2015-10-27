package ws.argo.probe.transport.sender.mqtt;

import org.junit.Test;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSender;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.probe.transport.responder.mqtt.MqttResponderTransport;

import java.net.*;
import java.util.Properties;

/**
 * Created by jmsimpson on 10/19/15.
 */

public class MqttSenderTransportTest {


    @Test
    public void testInitializingMqttSenderTransport() throws TransportConfigException, TransportException {
        Properties p = new Properties();

        p.put("mqttTopic", "/argo/test");
        p.put("qos", "0");
        p.put("broker", "tcp://localhost:1883");
        p.put("clientId", "mqttTestClient");

        MqttSenderTransport senderTransport = new MqttSenderTransport();

        senderTransport.initialize(p, "");

        senderTransport.close();


    }

    @Test
    public void testSendingProbe() throws UnknownHostException, SocketException, TransportConfigException, UnsupportedPayloadType, MalformedURLException, ProbeSenderException, TransportException {
        Transport transport = new MqttSenderTransport();

        NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        String macAddr = String.valueOf(ni.getHardwareAddress());
        String clientId = "JUnit Test Client " + macAddr;

        Properties senderProps = new Properties();
        senderProps.put("broker", "tcp://localhost:1883");
        senderProps.put("clientId", clientId);

        transport.initialize(senderProps, "");
        ProbeSender sender = new ProbeSender(transport);

        Probe probe = new Probe(Probe.JSON);
        probe.addRespondToURL("bad", "http://" + InetAddress.getLocalHost().getHostAddress() + ":4009");

        probe.setClientID(clientId);
        sender.sendProbe(probe);

        sender.close();
    }

}
