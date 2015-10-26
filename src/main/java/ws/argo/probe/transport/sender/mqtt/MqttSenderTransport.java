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

package ws.argo.probe.transport.sender.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSenderException;

import javax.xml.bind.JAXBException;
import java.util.Properties;
import java.util.logging.Logger;

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
    private String _topicName;
    private MqttTopic _topic;
    private int _qos;
    private String _broker;
    private String _clientId;
    private MemoryPersistence _persistence;
    private MqttClient _mqttClient;
    private MqttConnectOptions _connOpts;

    /**
     * Initialize the transport with the values provided in the Properties object.
     *
     * @param p                the Properties object with the initialization values
     * @param networkInterface name of the network interface
     * @throws TransportConfigException if something goes wrong
     */
    public void initialize(Properties p, String networkInterface) throws TransportConfigException {

        // we can ignore the network interface name here

         _topicName = p.getProperty("mqttTopic", DEFAULT_TOPIC);
        try {
            _qos = parseInt(p.getProperty("qos", "0"));
        } catch (NumberFormatException e) {
            LOGGER.warning("Issue parsing the QOS [" + p.getProperty("qos") + "].  Using default of 0.");
            _qos = 0;
        }
        _broker = p.getProperty("broker");
        _clientId = p.getProperty("clientId", "NO CLIENT ID");

        if (_broker == null) {
            throw new TransportConfigException("The broker MUST be configured correctly");
        }

        if (_topicName.equals(DEFAULT_TOPIC)) {
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
            message.setRetained(false);

            MqttDeliveryToken token;
            try {
                token = _topic.publish(message);
                // Wait until the message has been delivered to the broker
                token.waitForCompletion(); // might want a timeout here
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
        _connOpts = new MqttConnectOptions();

        _connOpts.setCleanSession(true);
        _connOpts.setKeepAliveInterval(30);
//        connOpt.setUserName(M2MIO_USERNAME);
//        connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());

        _persistence = new MemoryPersistence();

        try {

            _mqttClient = new MqttClient(_broker, _clientId, _persistence);
            LOGGER.fine("Connecting to broker [" + _broker + "]");
            _mqttClient.connect(_connOpts);

            _topic = _mqttClient.getTopic(_topicName);

            LOGGER.fine("Connected MQTT Client [" + _clientId + "] to broker [" + _broker + "]");
        } catch (MqttException me) {
            throw new TransportConfigException("Error connecting [" + _clientId + "] broker [" + _broker + "]", me);
        }
    }
}
