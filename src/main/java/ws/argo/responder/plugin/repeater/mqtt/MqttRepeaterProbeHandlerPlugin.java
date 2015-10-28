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


package ws.argo.responder.plugin.repeater.mqtt;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import ws.argo.plugin.probehandler.ProbeHandlerConfigException;
import ws.argo.plugin.probehandler.ProbeHandlerPlugin;
import ws.argo.plugin.transport.exception.TransportConfigException;
import ws.argo.plugin.transport.exception.TransportException;
import ws.argo.plugin.transport.sender.Transport;
import ws.argo.probe.Probe;
import ws.argo.probe.ProbeSender;
import ws.argo.probe.ProbeSenderException;
import ws.argo.probe.transport.sender.mqtt.MqttSenderTransport;
import ws.argo.wireline.probe.ProbeWrapper;
import ws.argo.wireline.probe.XMLSerializer;
import ws.argo.wireline.response.ResponseWrapper;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;

/**
 * The MqttRepeaterProbeHandlerPlugin does pretty much what it says.  It takes probes
 * that were handled by the Responder via whatever transports it has configured and then
 * repeats them out onto a MQTT Sender transport that is separately configured as part
 * of this probe handler plugin.
 *
 * <p>This handler should be used in handling discovery domain gateways.  However, if
 * this repeater resends the probe on the same MQTT channel as it received it, it could
 * cause very odd behavior.  It should NOT resend on a channel that would cause a loop
 * back or other feedback issues.
 *
 * Created by jmsimpson on 10/27/15.
 */
public class MqttRepeaterProbeHandlerPlugin implements ProbeHandlerPlugin {

    private static final Logger LOGGER = Logger.getLogger(MqttRepeaterProbeHandlerPlugin.class.getName());

    // Properties
    private ProbeSender _sender;


    /**
     * The probe handler will simply resend the probe via the sender it setup with it
     * initialized.  It then will return an empty response back to the Responder as
     * it isn't actually finding any services to return.
     *
     * @param probeWrapper the wireline probe payload to repeat
     * @return the empty ResponseWrapper
     */
    @Override
    public ResponseWrapper handleProbeEvent(ProbeWrapper probeWrapper) {
        LOGGER.fine("MqttRepeaterProbeHandlerPlugin handling probe: " + probeWrapper.asXML());

        ResponseWrapper response = new ResponseWrapper(probeWrapper.getProbeId());

        Probe probe = new Probe(probeWrapper);
        try {
            _sender.sendProbe(probe);
        } catch (ProbeSenderException e) {
            LOGGER.log(Level.WARNING, "Unable to repeat probe to MQTT Transport.", e);
        }

        return response;
    }

    /**
     * Read in the XML configuration file provided.  This should contain the data it needs to
     * make the MQTT connection and setup the probe sender.
     *
     * @param xmlConfigFilename the name of the XML config file
     * @throws ProbeHandlerConfigException if something goes wrong
     */
    @Override
    public void initializeWithPropertiesFilename(String xmlConfigFilename) throws ProbeHandlerConfigException {

        Properties properties;
        try {
            properties = processPropertiesFile(xmlConfigFilename);
            initializeProbeSender(properties);
        } catch (TransportConfigException e) {
            throw new ProbeHandlerConfigException("Error reading config [" + xmlConfigFilename + "]", e);
        } catch (SocketException | UnknownHostException e) {
            throw new ProbeHandlerConfigException("Error initializing MQTT Repeater", e);
        }


    }

    private void initializeProbeSender(Properties properties) throws UnknownHostException, SocketException, TransportConfigException {
        Transport transport = new MqttSenderTransport();

        NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        String macAddr = ni.getHardwareAddress().toString();
        String clientId = "MQTT-Repeater-Client-" + macAddr;

        properties.put("clientId", clientId);

        transport.initialize(properties, "");
        _sender = new ProbeSender(transport);

    }

    /**
     * Digs through the xml file to get the particular configuration items necessary to
     * run this responder transport.
     *
     * @param xmlConfigFilename the name of the xml configuration file
     * @return an XMLConfiguration object
     * @throws TransportConfigException if something goes awry
     */
    private Properties processPropertiesFile(String xmlConfigFilename) throws TransportConfigException {

        Properties props = new Properties();
        XMLConfiguration config;

        try {
            config = new XMLConfiguration(xmlConfigFilename);
        } catch (ConfigurationException e) {
            throw new TransportConfigException(e.getLocalizedMessage(), e);
        }

        props.put("mqttTopic", config.getString("mqttTopic"));
        props.put("qos", config.getString("qos", "0"));

        props.put("broker", config.getString("broker"));
        props.put("clientId", config.getString("clientId", "NO CLIENT ID"));

        if (config.getString("username") != null)
            props.put("username", config.getString("username"));
        if (config.getString("password") != null)
            props.put("password", config.getString("password"));

        return props;
    }

}
