package ws.argo.probe.transport.sender.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.plugin.transport.sender.TransportConfigException;
import ws.argo.plugin.transport.sender.TransportException;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;

import javax.xml.bind.JAXBException;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;

/**
 * The MQTT probe Transport will support sending Argo probes via a MQTT broker.
 * <p>
 * Created by jmsimpson on 10/16/15.
 */
public class MqttSenderTransport implements Transport {

    private static final String DEFAULT_TOPIC = "mqtt_default";

    private static final Logger LOGGER = Logger.getLogger(MqttSenderTransport.class.getName());

    // Properties
    private String _topic = "MQTT Examples";
    private int _qos = 2;
    private String _broker = "tcp://localhost:1883";
    private String _clientId = "JavaSample";
    private MemoryPersistence _persistence;
    private MqttClient _mqttClient;


    /**
     * Initialize the transport with the values provided in the Properties object.
     *
     * @param p                the Properties object with the initialization values
     * @param networkInterface name of the network interface
     * @throws TransportConfigException if something goes wrong
     */
    public void initialize(Properties p, String networkInterface) throws TransportConfigException {

        // we can ignore the network interface name here

        _persistence = new MemoryPersistence();

        _topic = p.getProperty("mqttTopic", DEFAULT_TOPIC);
        try {
            _qos = parseInt(p.getProperty("qos", "0"));
        } catch (NumberFormatException e) {
            LOGGER.warning("Issue parsing the QOS [" + p.getProperty("qos") + "].  Using default of 0.");
            _qos = 0;
        }
        _broker = p.getProperty("broker");
        _clientId = p.getProperty("clientID", "NO CLIENT ID");

        if (_broker == null) {
            throw new TransportConfigException("The broker MUST be configured correctly");
        }

        if (_topic.equals(DEFAULT_TOPIC)) {
            LOGGER.info("MQTT topic not defined.  Using the default MQTT Topic [" + DEFAULT_TOPIC + "]");
        }

        createMQTTConnection();

    }

    /**
     * Actually send the probe out on transport mechanism.
     *
     * @param probe the Probe instance that has been pre-configured
     * @throws ProbeSenderException if something bad happened when sending the
     *                              probe
     */
    public void sendProbe(Probe probe) throws TransportException {

        try {
            MqttMessage message = new MqttMessage(probe.asXML().getBytes());
            message.setQos(_qos);

            try {
                _mqttClient.publish(_topic, message);
            } catch (MqttException e) {
                throw new TransportException("Error publishing message to broker [" + _broker + "]", e);
            }

            LOGGER.fine("Probe published on broker [" + _broker + "] by [" + _clientId + "]");
        } catch (JAXBException e) {
            throw new TransportException("Unable to send probe because it could not be serialized to XML", e);
        }
    }

    /**
     * Return the maximum payload size that this transport can handle. For
     * example, the payload of the UDP Multicast transport could only be 600
     * bytes, meaning that the probe might be split up into several smaller
     * probes. But other transports such as JMS or SNS might allow probe payload
     * sizes much larger (practically unlimited).
     *
     * @return max payload size in bytes
     */
    public int maxPayloadSize() {
        return 250000000;
    }

    public String getNetworkInterfaceName() {
        return null;
    }

    /**
     * Close the transport.
     *
     * @throws ProbeSenderException if something bad happened
     */
    public void close() throws TransportException {
        try {
            _mqttClient.disconnect();
        } catch (MqttException e) {
            throw new TransportException("Error closing [" + _clientId + "] on broker [" + _broker + "]", e);
        }
        LOGGER.fine("Disconnected from MQTT: Client [" + _clientId + "] on broker [" + _broker + "]");
    }

    private void createMQTTConnection() throws TransportConfigException {
        try {
            _mqttClient = new MqttClient(_broker, _clientId, _persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            LOGGER.fine("Connecting to broker [" + _broker + "]");
            _mqttClient.connect(connOpts);
            LOGGER.fine("Connected MQTT Client [" + _clientId + "] to broker [" + _broker + "]");
        } catch (MqttException me) {
            throw new TransportConfigException("Error connecting [" + _clientId + "] broker [" + _broker + "]", me);
        }
    }
}
