/*
 * Copyright 2015 Jeff Simpson.
 *
 * This file is part of the Argo MQTT Transport plugin.
 *
 * Argo MQTT Transport plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

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

import java.util.Timer;
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
    private String _topicName;
    private int _qos;
    private String _broker;
    private String _clientId;
    private String _username;
    private String _password;
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

        setupReconnectTimer();
    }

    private void setupReconnectTimer() {

        Timer reconnectTimer = new Timer();


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
     * This run method will actually just wait while the MQTT client waits for inbound
     * messages.
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
        LOGGER.warning("MQTT Transport Connection Lost: " + throwable.getMessage());
        LOGGER.info("MQTT Transport attempting to reconnect to " + _broker);
        try {
            createMQTTConnection();
        } catch (TransportConfigException e) {
            LOGGER.warning("MQTT unable to reconnect: " + throwable.getMessage());
        }
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
            LOGGER.fine("MQTT Transport Delivery Complete: " + iMqttDeliveryToken.getMessage());
        } catch (MqttException e) {
            LOGGER.log(Level.WARNING, "Error logging deliveryComplete message - ", e);
        }

    }

    /**
     * Digs througs the xml file to get the particular configuration items necessary to
     * run this responder transport.
     *
     * @param xmlConfigFilename the name of the xml configuration file
     * @return an XMLConfiguration object
     * @throws TransportConfigException if something goes awry
     */
    private void processPropertiesFile(String xmlConfigFilename) throws TransportConfigException {

        XMLConfiguration config;

        try {
            config = new XMLConfiguration(xmlConfigFilename);
        } catch (ConfigurationException e) {
            throw new TransportConfigException(e.getLocalizedMessage(), e);
        }

        _topicName = config.getString("mqttTopic", DEFAULT_TOPIC);

        try {
            _qos = parseInt(config.getString("qos", "0"));
        } catch (NumberFormatException e) {
            LOGGER.warning("Issue parsing the QOS [" + config.getString("qos") + "].  Using default of 0.");
            _qos = 0;
        }
        _broker = config.getString("broker");
        _clientId = config.getString("clientId", "NO CLIENT ID");

        if (_broker == null || _broker.isEmpty()) {
            // probably need to check validity
            throw new TransportConfigException("The broker MUST be configured correctly");
        }

        if (_topicName.equals(DEFAULT_TOPIC)) {
            LOGGER.info("MQTT topic not defined.  Using the default MQTT Topic [" + DEFAULT_TOPIC + "]");
        }

        _username = config.getString("username");
        _password = config.getString("password");

    }


    private void createMQTTConnection() throws TransportConfigException {
        _connOpts = new MqttConnectOptions();

        _connOpts.setCleanSession(true);
        _connOpts.setKeepAliveInterval(30);

        if (_username != null && !_username.isEmpty())
            _connOpts.setUserName(_username);
        if (_password != null && !_password.isEmpty())
            _connOpts.setPassword(_password.toCharArray());

        _persistence = new MemoryPersistence();

        try {
            _mqttClient = new MqttClient(_broker, _clientId, _persistence);

            LOGGER.fine("Connecting to broker [" + _broker + "]");
            _mqttClient.connect(_connOpts);
            _mqttClient.setCallback(this);

            LOGGER.fine("Connected MQTT Client [" + _clientId + "] to broker [" + _broker + "]");
        } catch (MqttException me) {
            throw new TransportConfigException("Error connecting [" + _clientId + "] broker [" + _broker + "]", me);
        }

        try {
            _mqttClient.subscribe(_topicName, _qos);
        } catch (MqttException e) {
            throw new TransportConfigException("Error subscribing to topic [" + _topicName + "]");
        }

    }

}
