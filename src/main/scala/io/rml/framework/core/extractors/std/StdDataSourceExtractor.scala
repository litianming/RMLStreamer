/**
  * MIT License
  *
  * Copyright (C) 2017 - 2020 RDF Mapping Language (RML)
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  * THE SOFTWARE.
  *
  **/

package io.rml.framework.core.extractors.std

import io.rml.framework.core.extractors.DataSourceExtractor
import io.rml.framework.core.extractors.ExtractorUtil.{extractLiteralFromProperty, extractResourceFromProperty, extractSingleLiteralFromProperty, extractSingleResourceFromProperty}
import io.rml.framework.core.model._
import io.rml.framework.core.model.rdf.RDFResource
import io.rml.framework.core.vocabulary.{HypermediaVoc, RDFVoc, RMLVoc, WoTVoc}
import io.rml.framework.shared.RMLException

class StdDataSourceExtractor extends DataSourceExtractor {

  /**
    * Extracts a data source from a resource.
    *
    * @param node Resource to extract data source from.
    * @return
    */
  override def extract(node: RDFResource): DataSource = {

    val property = RMLVoc.Property.SOURCE
    val properties = node.listProperties(property)

    if (properties.size != 1) throw new RMLException(node.uri + ": only one data source allowed.")

    properties.head match {
      case literal: Literal => FileDataSource(literal) // the literal represents a path uri
      case resource: RDFResource => extractDataSourceFromResource(resource)
    }

  }

  /**
    * Retrieves data source properties from a resource that represents a data source.
    *
    * @param resource Resource that represents a data source.
    * @return
    */
  private def extractDataSourceFromResource(resource: RDFResource): DataSource = {
    val property = RDFVoc.Property.TYPE
    val properties = resource.listProperties(property)
    if (properties.size != 1) throw new RMLException(resource.uri + ": type must be given.")
    properties.head match {
      case classResource: RDFResource => classResource.uri match {
        case Uri(RMLVoc.Class.TCPSOCKETSTREAM) => extractTCPSocketStream(resource)
        case Uri(RMLVoc.Class.FILESTREAM) => extractFileStream(resource)
        case Uri(RMLVoc.Class.KAFKASTREAM) => extractKafkaStream(resource)
        case Uri(WoTVoc.ThingDescription.Class.THING) => extractWoTSource(resource)
      }
      case literal: Literal => throw new RMLException(literal.value + ": type must be a resource.")
    }
  }

  private def extractFileStream(resource: RDFResource): StreamDataSource = {
    val path = extractSingleLiteralFromProperty(resource, RMLVoc.Property.PATH)
    FileStream(path)
  }

  private def extractKafkaStream(resource: RDFResource): StreamDataSource = {
    val broker = extractSingleLiteralFromProperty(resource, RMLVoc.Property.BROKER)
    val groupId = extractSingleLiteralFromProperty(resource, RMLVoc.Property.GROUPID)
    val topic = extractSingleLiteralFromProperty(resource, RMLVoc.Property.TOPIC)

    KafkaStream(List(broker), groupId, topic)
  }

  private def extractTCPSocketStream(resource: RDFResource): StreamDataSource = {
    val hostName = extractSingleLiteralFromProperty(resource, RMLVoc.Property.HOSTNAME)
    val port = extractSingleLiteralFromProperty(resource, RMLVoc.Property.PORT)

    val _type = extractSingleLiteralFromProperty(resource, RMLVoc.Property.TYPE)
    TCPSocketStream(hostName, port.toInt, _type)
  }

  private def extractWoTSource(resource: RDFResource): DataSource = {
    // A WoT Thing contains (in our case) a PropertyAffordance, which contains a form describing how to access the real source

    val propertyAffordance = extractSingleResourceFromProperty(resource, WoTVoc.ThingDescription.Property.HASPROPERTYAFFORDANCE);
    val form = extractSingleResourceFromProperty(propertyAffordance, WoTVoc.ThingDescription.Property.HASFORM);

    // extract info from form

    // extract the hypermedia target (~uri)
    val hypermediaTarget = extractSingleLiteralFromProperty(form, HypermediaVoc.Property.HASTARGET);

    // extract the desired content type
    val contentType = extractSingleLiteralFromProperty(form, HypermediaVoc.Property.FORCONTENTTYPE);

    // now check for soure type (MQTT, HTTP, ...)
    val isMQTT = form.hasPredicateWith(WoTVoc.WoTMQTT.namespace._2);
    if (isMQTT) {
      return extractWoTMQTTSource(form, hypermediaTarget, contentType);
    }

    // TODO replace with real source
    FileDataSource(Literal("/tmp/test"))
  }

  private def extractWoTMQTTSource(form: RDFResource, hypermediaTarget: String, contentType: String): DataSource = {
    val controlPacketValue = extractLiteralFromProperty(form, WoTVoc.WoTMQTT.Property.CONTROLPACKETVALUE, "SUBSCRIBE");

    var qosOpt: Option[String] = None;
    var dup: Boolean = false;
    val mqttOptions = extractResourceFromProperty(form, WoTVoc.WoTMQTT.Property.OPTIONS);
    if (mqttOptions.isDefined) {
      // extract the actual values
      val mqttOptionsResource = mqttOptions.get;
      mqttOptionsResource.getList
        .map(rdfNode => rdfNode.asInstanceOf[RDFResource])
        .foreach(mqttOptionsResource => {
          val optionName = extractSingleLiteralFromProperty(mqttOptionsResource, WoTVoc.WoTMQTT.Property.OPTIONNAME);
          optionName match {
            case "qos" => qosOpt = Some(extractSingleLiteralFromProperty(mqttOptionsResource, WoTVoc.WoTMQTT.Property.OPTIONVALUE));
            case "dup" => dup = true;
          };
        });
    }

    // TODO make actual data source
    logWarning("Here a MQTT data source will be created. hypermediaTarget: " + hypermediaTarget
      + ", contentType: " + contentType + ", dup: " + dup + ", qusOpt: " + qosOpt)
    FileDataSource(Literal("/tmp/test"))
  }

}
