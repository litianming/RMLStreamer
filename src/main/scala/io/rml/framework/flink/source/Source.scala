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
package io.rml.framework.flink.source

import io.rml.framework.core.model.{FileDataSource, LogicalSource, StreamDataSource}
import io.rml.framework.core.vocabulary.RMLVoc
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

trait Source

/**
  * Object for generating Flink Sources from a LogicalSource
  */
object Source {

  val DEFAULT_ITERATOR_MAP: Map[String, String] =  Map(
    RMLVoc.Class.JSONPATH -> "$",
    RMLVoc.Class.CSV -> "",
    RMLVoc.Class.XPATH -> "/*"
  )

  val DEFAULT_ITERATOR_SET: Set[String] = DEFAULT_ITERATOR_MAP.values.toSet

  def apply(logicalSource: LogicalSource)(implicit env: ExecutionEnvironment, senv: StreamExecutionEnvironment): Source = {
    logicalSource.source match {
      case fs: FileDataSource => FileDataSet(logicalSource)
      case ss: StreamDataSource => StreamDataSource.fromLogicalSource(logicalSource)
    }
  }

}
