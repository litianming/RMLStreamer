package io.rml.framework

import java.io.File

import io.rml.framework.core.extractors.MappingReader
import io.rml.framework.core.model.FormattedRMLMapping
import io.rml.framework.helper.Logger
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object Test extends App {

  implicit val env = ExecutionEnvironment.getExecutionEnvironment
  implicit val senv = StreamExecutionEnvironment.getExecutionEnvironment
  val classLoader = getClass.getClassLoader
  val file = new File(classLoader.getResource("rml-testcases/RMLTC0007d-CSV/mapping.ttl").getFile)
  val mapping = MappingReader().read(file)

  val formattedMapping = FormattedRMLMapping.fromRMLMapping(mapping)
  Logger.logInfo("" + formattedMapping.standardTripleMaps.size)
  val result = Main.createDataSetFromFormattedMapping(formattedMapping).collect().reduce((a, b) => a + "\n" + b)
  System.out.println(result)
}