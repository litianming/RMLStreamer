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

package io.rml.framework.engine.statement


import io.rml.framework.api.RMLEnvironment
import io.rml.framework.core.model._
import io.rml.framework.core.util.Util
import io.rml.framework.engine.Engine
import io.rml.framework.flink.item.Item

/**
  *
  */
object TermMapGenerators {

  def constantUriGenerator(constant: Entity): Item => Option[Iterable[Uri]] = {
    // return a function that just returns the constant
    (item: Item) => {
      Some(List(Uri(constant.toString)))
    }
  }

  def constantLiteralGenerator(constant: Entity, datatype: Option[Uri] = None, language: Option[Literal]): Item => Option[Iterable[Literal]] = {
    // return a function that just returns the constant
    item: Item => {
      Some(List(Literal(constant.toString, datatype, language)))
    }

  }

  def templateUriGenerator(termMap: TermMap): Item => Option[Iterable[Uri]] = {
    // return a function that processes the template
    item: Item => {

      for {
        iter <- Engine.processTemplate(termMap.template.get, item, encode = true)
      } yield for {
        value <- iter
        processed <- processIRI(value)
        uri = Uri(processed)
      } yield uri
    }
  }

  def templateLiteralGenerator(termMap: TermMap): Item => Option[Iterable[Literal]] = {
    // return a function that processes the template
    item: Item => {
      for {
        iter <- Engine.processTemplate(termMap.template.get, item)
      } yield for {
        value <- iter
        lit = Literal(value, termMap.datatype, termMap.language)

      } yield lit
    }
  }

  def templateBlankNodeGenerator(termMap: TermMap): Item => Option[Iterable[Blank]] = {
    item: Item => {

      for {
        iter <- Engine.processTemplate(termMap.template.get, item, encode = true)
      } yield for {
        value <- iter
        blank = Blank(value)
      } yield blank
    }
  }

  def referenceLiteralGenerator(termMap: TermMap): Item => Option[Iterable[Literal]] = {
    // return a function that processes a reference
    item: Item => {
      for {
        iter <- Engine.processReference(termMap.reference.get, item)

      } yield for {
        value <- iter
        lit = Literal(value, termMap.datatype, termMap.language)

      } yield lit
    }
  }

  def referenceUriGenerator(termMap: TermMap): Item => Option[Iterable[Uri]] = {
    // return a function that processes a reference

    item: Item => {
      for {
        iter <- Engine.processReference(termMap.reference.get, item)

      } yield for {
        iri <- iter
        processed <- processIRI(iri)
        uri = Uri(processed)
      } yield uri
    }
  }

  private def processIRI(origIri: String): Iterable[String] =  {
    if (Util.isValidAbsoluteUri(origIri)) {
      List(origIri)
    } else {
      val baseIRI = RMLEnvironment.getGeneratorBaseIRI()
      if (baseIRI.isDefined) {
        val completeUri = baseIRI.get + origIri
        if (Util.isValidAbsoluteUri(completeUri)) {
          List(completeUri)
        } else {
          List()
        }
      } else {
        List()
      }
    }
  }

}
