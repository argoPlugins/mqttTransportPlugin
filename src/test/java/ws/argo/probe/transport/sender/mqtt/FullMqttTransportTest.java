package ws.argo.probe.transport.sender.mqtt;

import org.junit.Test;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.plugin.transport.responder.ProbeProcessor;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSender;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.probe.transport.responder.mqtt.MqttResponderTransport;
import ws.argo.wireline.probe.ProbeWrapper;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jmsimpson on 10/23/15.
 * @author jmsimpson
 */
public class FullMqttTransportTest {

    private static final Logger LOGGER = Logger.getLogger(FullMqttTransportTest.class.getName());


    @Test
    public void testFullTransportTest() throws UnknownHostException, SocketException, TransportConfigException, UnsupportedPayloadType, MalformedURLException, ProbeSenderException, TransportException {
        ProbeProcessor probeProcessor = new ProbeProcessor() {
            @Override
            public void processProbe(ProbeWrapper probeWrapper) {
                LOGGER.info("Probe Processor processing probe " + probeWrapper.getProbeId());
                LOGGER.info("Probe : " + probeWrapper.asXML());
            }

            @Override
            public float probesPerSecond() {
                return 0;
            }

            @Override
            public int probesProcessed() {
                return 0;
            }

            @Override
            public void probeProcessed() {

            }

            @Override
            public String getRuntimeID() {
                return null;
            }
        };

        final MqttResponderTransport responderTransport = new MqttResponderTransport();

        Runnable responder = new Runnable() {
            @Override
            public void run() {
                try {
                    responderTransport.initialize(probeProcessor, "testMqttResponderConfig.xml");
                    responderTransport.run();
                } catch (TransportConfigException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }


            }
        };

        LOGGER.setLevel(Level.ALL);
        responder.run();  // Got the responder up

        Transport transport = new MqttSenderTransport();

        NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        String macAddr = ni.getHardwareAddress().toString();
        String clientId = "JUnit-Probe-Sender-Client-" + macAddr;

        Properties senderProps = new Properties();
        senderProps.put("mqttTopic", "ws.argo/test");
        senderProps.put("broker", "tcp://localhost:1883");
        senderProps.put("clientId", clientId);

        transport.initialize(senderProps, "");
        ProbeSender sender = new ProbeSender(transport);

        Probe probe = new Probe(Probe.JSON);
        probe.addRespondToURL("bad", "http://" + InetAddress.getLocalHost().getHostAddress() + ":4009");

        probe.setClientID(clientId);
        sender.sendProbe(probe);

        sender.close();
        responderTransport.shutdown();
    }

}
