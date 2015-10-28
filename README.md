#Argo MQTT Transport Plugin

Copyright (c) Jeff Simpson, 2015

Licened under the GPL3 open source license.  Please see the LICENCE file for more information. 

These classes provide the Responder and Sender MQTT transports as well as the MQTT Repeater Probe Handler plugin.

The Argo runtime service discovery system utilizes a publish/subscribe messaging model with an asynchronous out-of-band response.  One popular pub/sub messaging system is MQTT (see [www.mqtt.org](www.mqtt.org)).  

##What is MQTT? 

> MQTT stands for MQ Telemetry Transport. It is a publish/subscribe, extremely simple and lightweight messaging protocol, designed for constrained devices and low-bandwidth, high-latency or unreliable networks. The design principles are to minimise network bandwidth and device resource requirements whilst also attempting to ensure reliability and some degree of assurance of delivery. These principles also turn out to make the protocol ideal of the emerging “machine-to-machine” (M2M) or “Internet of Things” world of connected devices, and for mobile applications where bandwidth and battery power are at a premium.

##Installing the Plugin
Installation should be painless.  In a nutshell:

1. Download the plugin from the [Github release site](https://github.com/argoPlugins/mqttTransportPlugin/releases/latest).
2. Copy the plugin `JAR` file and copy it into the `lib` directory of the Argo Responder and/or the Argo Command Line Client.  When you run the responder or the command line client, it will pick up any new `JAR` files in the `lib` directory.  The standard location for the `lib` directories on a Linux system for the Responder and Client are `/opt/argo/responder/lib` and `/opt/argo/client/lib` repectively.
3. Install the configuration `XML` files (one for the Transport and one  for the Repeater) into the `config` directory of the Argo Responder.

##Configure the Argo Responder
Adding the Transport plugin to the Argo Repsonder is pretty simple.

###Add the MQTT Responder Tranport to the Responder
If you want to have your Argo Repsonder configured to recieive probes using MQTT, you’ll need to setup Argo to use the MQTT Transport.  After installing the jar file and the configuration files in the appropriate places, you’ll need to edit the main `responderConfig.xml` file.
In the `<transports>` section, add the following transport configuration:

```xml
<responder>
  ...
  <transports>
    ...
    
    <transport>
      <classname>ws.argo.probe.transport.responder.mqtt.MQTTResponderTransport</classname>
      <configFilename>/opt/argo/responder/config/mqttTransportConfig.xml</configFilename>
    </transport>
    
    ...
  <transports>
  ...
</responder>
```
This will tell the Argo Responder to listen for incoming probes using the MQTT Tranport and will configure that transport according the the xml file.

###Configuring the MQTT Responder Tranport
The configuration file for the MQTT Responder Tranport looks like this:

```xml
<mqttTransport>
  <mqttTopic>ws.argo/test</mqttTopic>
  <qos>0</qos>
  <broker>tcp://localhost:1883</broker>
  <clientId>testResponderTransport</clientId>
  <username>user</username>
  <password>p@ssw0rd</password>
</mqttTransport>
```
This means that the MQTT Transport will listen for probe messages coming on the topic named `ws.argo/test`.  MQTT Topic are ephemeral, so they don’t have to be configured in advance in some broker as they do in JMS or MQ.

It will expect a Quality of Service of 0.  At most once (0), At least once (1), Exactly once (2).

It will attempt to connect to the specified broker.  The default configuration uses `tcp://localhost:1883` but, you should know where your broker is (or, of course, you could use Argo to discovery it).  But, really, you should know.

It will use the specified `clientId` to help identify who is connected to the server is case you need to do some maintenance or debugging.

An, of course, there is the `username` and `password`.  This version of the MQTT Transport Plugin only uses a `username` and `password` pair at the moment.  SSL Certificates will come in a future version.

##Configure the Argo Command Line Client

The Argo command line client can be configured to use the MQTT Transport to send probes using MQTT.  This can be very useful for testing the Responder configuration.

```xml
<client>
  ...
  <transports>
    ...
    
    <transport>
      <name>Multicast</name>
      <enableOnStartup>true</enableOnStartup>
      <usesNI>false</usesNI>
      <requiresMulticast>false</requiresMulticast>
      <classname>ws.argo.probe.transport.sender.mqtt.MQTTSenderTransport</classname>
      <configFilename>/opt/argo/responder/config/mqttTransportConfig.xml</configFilename>
    </transport>
    
    ...
  <transports>
  ...
</client>
```

###Configuring the MQTT Sender Tranport
Configuring the MQTT Sender Transport is the same as configuring the MQTT Responder Transport.  Please refer to that section for configuation.

##The MQTT Repeater Probe Handler

> __A Note on Repeater Probe Handlers__
> 
> Repeater Probe Handlers are used when you are creating a gateway between discovery domains.  The idea of a discovery domain is covered in greater detail on the main Argo wiki pages in Github.  However, in a nutshell, you use a Repeater when your probe transport needs to be bridged to another transport and/or another discovery channel.  For example, if you are doing “local” discovery using the “ws.argo/myLocalOrg/discovery’ topic on MQTT but the enterprise (or other domain) is using another topic or even another transport altogether (like Muticast), then you’d use the Repeater to get probes to publish into your discovery domain.

##Configure the Argo Responder
Adding the Probe Handler plugin to the Argo Repsonder is pretty simple.

If you want to have your Argo Repsonder configured to handle incoming probes using and repeat them out via MQTT, you’ll need to setup Argo to use the MQTT Repeater Probe Handler plugin.  After installing the jar file and the configuration files in the appropriate places, you’ll need to edit the main `responderConfig.xml` file.
In the `<probeHandlers>` section, add the following transport configuration:

```xml
<responder>
  ...
  <probeHandlers>
    ...

    <probeHandler>
      <classname>ws.argo.responder.plugin.repeater.mqtt.MqttRepeaterProbeHandlerPlugin</classname>
      <configFilename>/opt/argo/responder/config/mqttRepeaterConfig.xml</configFilename>
    </probeHandler>
    
    ...
  </probeHandlers>
  ...
</responder>
```
This will tell the Argo Responder to process incoming probes using the MQTT Repeater Plugin and will configure that transport according the the xml file.

###Configuring the MQTT Repeater Probe Handler

The configuration file for the MQTT Repeater Probe Handler looks like this:

```xml
<mqttTransport>
  <mqttTopic>ws.argo/test</mqttTopic>
  <qos>0</qos>
  <broker>tcp://localhost:1883</broker>
  <clientId>testResponderTransport</clientId>
  <username>user</username>
  <password>p@ssw0rd</password>
</mqttTransport>
```

Curiously, it’s identical to the configuration file for the MQTT Transport.  Please refer to that section for configuation.