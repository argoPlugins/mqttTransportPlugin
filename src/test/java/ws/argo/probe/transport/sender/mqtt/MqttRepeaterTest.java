package ws.argo.probe.transport.sender.mqtt;

import org.junit.Test;
import ws.argo.plugin.probehandler.ProbeHandlerConfigException;
import ws.argo.plugin.probehandler.ProbeHandlerPlugin;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.responder.ProbeProcessor;
import ws.argo.probe.Probe;
import ws.argo.probe.UnsupportedPayloadType;
import ws.argo.probe.transport.responder.mqtt.MqttResponderTransport;
import ws.argo.responder.plugin.repeater.mqtt.MqttRepeaterProbeHandlerPlugin;
import ws.argo.wireline.probe.ProbeParseException;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.XMLSerializer;

import javax.xml.bind.JAXBException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jmsimpson on 10/27/15.
 */
public class MqttRepeaterTest {

    private static final Logger LOGGER = Logger.getLogger(MqttRepeaterTest.class.getName());

    @Test
    public void testMqttRepeaterConfig() throws ProbeHandlerConfigException {

        ProbeHandlerPlugin handlerPlugin = new MqttRepeaterProbeHandlerPlugin();

        handlerPlugin.initializeWithPropertiesFilename("testMqttResponderConfig.xml");

    }

    @Test
    public void testMqttRepeaterNoUsernamePasswordConfig() throws ProbeHandlerConfigException {

        ProbeHandlerPlugin handlerPlugin = new MqttRepeaterProbeHandlerPlugin();

        handlerPlugin.initializeWithPropertiesFilename("testNoUsernamePasswordMqttResponderConfig.xml");

    }

    @Test
    public void testMqttRepeater() throws ProbeHandlerConfigException, UnsupportedPayloadType, JAXBException, ProbeParseException {
        ProbeProcessor probeProcessor = new ProbeProcessor() {
            @Override
            public void processProbe(ProbeWrapper probeWrapper) {
                LOGGER.info("MqttRepeater Processor processing probe " + probeWrapper.getProbeId());
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


        ProbeHandlerPlugin handlerPlugin = new MqttRepeaterProbeHandlerPlugin();

        handlerPlugin.initializeWithPropertiesFilename("testMqttResponderConfig.xml");

        Probe probe = new Probe(Probe.JSON);
        String xml = probe.asXML();

        XMLSerializer serializer = new XMLSerializer();
        ProbeWrapper probeWrapper = serializer.unmarshal(xml);

        handlerPlugin.handleProbeEvent(probeWrapper);

    }

}
