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

import io.rml.framework.core.extractors._
import io.rml.framework.core.internal.Logging
import io.rml.framework.core.model.PredicateObjectMap
import io.rml.framework.core.model.rdf.{RDFLiteral, RDFResource}
import io.rml.framework.core.vocabulary.RMLVoc
import io.rml.framework.shared.RMLException

/**
  * Extractor for predicate object maps.
  */
class StdPredicateObjectMapExtractor(predicateMapExtractor: PredicateMapExtractor,
                                     objectMapExtractor: ObjectMapExtractor,
                                     functionMapExtractor: FunctionMapExtractor,
                                     graphMapExtractor: GraphMapExtractor)

  extends PredicateObjectMapExtractor with Logging {

  /**
    * Extracts predicate object maps from a resource.
    *
    * @param node Node to extract from.
    * @return
    */
  override def extract(node: RDFResource): List[PredicateObjectMap] = {

    logDebug(node.uri + ": Extracting predicate object maps.")

    // filter all predicate object map resources
    val properties = node.listProperties(RMLVoc.Property.PREDICATEOBJECTMAP)

    // iterate over all found predicate object map resources
    properties.map {
      case literal: RDFLiteral =>
        throw new RMLException(literal.toString +
          ": A literal cannot be converted to a predicate object map")

      case resource: RDFResource => extractPredicateObjectMap(resource)
    }

  }

  /**
    * Extracts predicate object map properties from a predicate object map resource.
    *
    * @param resource Resource that represents a predicate object map.
    * @return
    */
  private def extractPredicateObjectMap(resource: RDFResource): PredicateObjectMap = {
    this.logDebug("extractPredicateObjectMap : extracting object maps")
    val objectMaps = objectMapExtractor.extract(resource)

    this.logDebug("extractPredicateObjectMap : extracting function maps")
    val functionMaps = functionMapExtractor.extract(resource) // TODO: not used. can be removed?

    this.logDebug("extractPredicateObjectMap : extracting predicate maps")
    val predicateMaps = predicateMapExtractor.extract(resource)

    this.logDebug("extractPredicateObjectMap : extracting graph map")
    val graphMap = graphMapExtractor.extract(resource)

    this.logDebug("extractPredicateObjectMap : returning resulting PredicateObjectMap")
    PredicateObjectMap(resource.uri.toString, objectMaps, predicateMaps, graphMap)
  }

}
