package ws.argo.probe.transport.responder.mqtt;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import ws.argo.plugin.transport.responder.ProbeProcessor;
import ws.argo.plugin.transport.responder.Transport;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.wireline.probe.ProbeParseException;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.XMLSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;

/**
 * Created by jmsimpson on 10/19/15.
 */
public class MqttResponderTransport implements Transport, MqttCallback {

    private static final String DEFAULT_TOPIC = "mqtt_default";

    private static final Logger LOGGER = Logger.getLogger(MqttResponderTransport.class.getName());

    // Properties
    private String _topic;
    private int _qos;
    private String _broker;
    private String _clientId;
    private int _keepAliveInterval;
    private MemoryPersistence _persistence;
    private MqttClient _mqttClient;

    private boolean _isRunning;
    private final Object lock = new Object();
    private ProbeProcessor _processor;
    private MqttConnectOptions _connOpts;

    @Override
    public void initialize(ProbeProcessor probeProcessor, String propertiesFilename) throws TransportConfigException {
        this._processor = probeProcessor;

        processPropertiesFile(propertiesFilename);
        createMQTTConnection();
        try {
            _mqttClient.subscribe(_topic, _qos);
        } catch (MqttException e) {
            throw new TransportConfigException("Error subscribing to topic [" + _topic + "]");
        }
    }

    @Override
    public void shutdown() throws TransportException {
        _isRunning = false; // shutdown the run loop
        synchronized (lock){
            lock.notifyAll();
        }

        try {
            _mqttClient.disconnect();
        } catch (MqttException e) {
            throw new TransportException("Error closing [" + _clientId + "] on broker [" + _broker + "]", e);
        }
        LOGGER.fine("Disconnected from MQTT: Client [" + _clientId + "] on broker [" + _broker + "]");
    }

    @Override
    public String transportName() {
        return "MQTT";
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {


        LOGGER.fine("MQTT Responder now running - " + _clientId);
        while (_isRunning) {
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

        }

    }

    @Override
    public void connectionLost(Throwable throwable) {
        LOGGER.info("MQTT Transport Connection List: " + throwable.getMessage());
        System.out.println("MQTT Transport Connection List: " + throwable.getMessage());

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
//        System.out.println("MQTT Transport Received message: " + mqttMessage.toString());
        try {
            XMLSerializer serializer = new XMLSerializer();

            ProbeWrapper probe = serializer.unmarshal(mqttMessage.toString());

            _processor.processProbe(probe);

        } catch (ProbeParseException e) {
            LOGGER.log(Level.SEVERE, "Error parsing inbound probe payload.", e);
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        try {
            System.out.println("MQTT Transport Delivery Complete: " + iMqttDeliveryToken.getMessage());
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    private XMLConfiguration processPropertiesFile(String xmlConfigFilename) throws TransportConfigException {

        XMLConfiguration config;

        try {
            config = new XMLConfiguration(xmlConfigFilename);
        } catch (ConfigurationException e) {
            throw new TransportConfigException(e.getLocalizedMessage(), e);
        }

        _topic = config.getString("mqttTopic", DEFAULT_TOPIC);

        try {
            _qos = parseInt(config.getString("qos", "0"));
        } catch (NumberFormatException e) {
            LOGGER.warning("Issue parsing the QOS [" + config.getString("qos") + "].  Using default of 0.");
            _qos = 0;
        }
        _broker = config.getString("broker");
        _clientId = config.getString("clientId", "NO CLIENT ID");

        if (_broker == null || _broker.isEmpty()) {
            // propably need to check validity
            throw new TransportConfigException("The broker MUST be configured correctly");
        }

        if (_topic.equals(DEFAULT_TOPIC)) {
            LOGGER.info("MQTT topic not defined.  Using the default MQTT Topic [" + DEFAULT_TOPIC + "]");
        }

        return config;

    }


    private void createMQTTConnection() throws TransportConfigException {
        _connOpts = new MqttConnectOptions();

        _connOpts.setCleanSession(true);
        _connOpts.setKeepAliveInterval(30);
//        connOpt.setUserName(M2MIO_USERNAME);
//        connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());

        try {
            _mqttClient = new MqttClient(_broker, _clientId, _persistence);

            LOGGER.fine("Connecting to broker [" + _broker + "]");
            _mqttClient.connect(_connOpts);
            _mqttClient.setCallback(this);
            LOGGER.fine("Connected MQTT Client [" + _clientId + "] to broker [" + _broker + "]");
        } catch (MqttException me) {
            throw new TransportConfigException("Error connecting [" + _clientId + "] broker [" + _broker + "]", me);
        }
    }

}
