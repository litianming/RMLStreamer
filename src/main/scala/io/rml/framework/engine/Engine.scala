/*
 * Copyright (c) 2017 Ghent University - imec
 *
 * Permission is hereby granted, free of charge, to any person obtai ning a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.rml.framework.engine

import io.rml.framework.core.internal.Logging
import io.rml.framework.core.model.{Literal, Uri}
import io.rml.framework.flink.item.Item
import io.rml.framework.flink.sink.FlinkRDFQuad

import scala.collection.mutable

/**
  * Created by wmaroy on 22.08.17.
  */
trait Engine[T] extends Serializable {

  def process(item: T): List[FlinkRDFQuad]

}

object Engine extends Logging {


  /**
    * Retrieve reference included in a template from the given item.
    *
    * @param template
    * @param item
    * @return
    */
  def processTemplate(template: Literal, item: Item, encode: Boolean = false): Option[Iterable[String]] = {
    val regex = "(\\{[^\\{\\}]*\\})".r
    val replaced = template.value.replaceAll("\\$", "#")
    val result: mutable.Queue[String] = mutable.Queue.empty[String]
    val matches = regex.findAllMatchIn(replaced)


    // Matches is ie List("{foo}", "{bar}")
    for (m <- matches) {
      val sanitizedRef = removeBrackets(m.toString()).replaceAll("#", "\\$")
      val optReferred = if (encode) item.refer(sanitizedRef).map(lString => lString.map(el => Uri.encode(el))) else item.refer(sanitizedRef)
      //This is to get the value of Some(refList) (Not a for-loop over list!!!)
      optReferred.foreach(refList => {

        // Using fifo queue to create a list of template string with the combination of referenced resources
          if(result.isEmpty){
            //Used string template since we still have to escape the curly brackets
            result ++=  refList.map( referred =>  replaced.replaceAll(s"\\{$sanitizedRef\\}",referred))
          }else{

            // Previously edited templates need to be reused for combination with new referenced resources
            var count = 0
            val maxLen = result.length
            while (count !=  maxLen) {
              val candid =  result.dequeue()
              result  ++=  refList.map(referred =>  candid.replaceAll(s"\\{$sanitizedRef\\}",  referred))

              count += 1
            }


          }

      })
    }

    if(result.isEmpty) None else Some(result)

    //    val result = regex.replaceAllIn(replaced, m => {
    //      val reference = removeBrackets(m.toString()).replaceAll("#", "\\$")
    //
    //      val referred = if (encode)
    //      val referred = if (encode) item.refer(reference).flatMap(referred => Some(Uri.encode(referred))) else item.refer(reference) // if encode, this means this is an Uri
    //      if (referred.isDefined) referred.get
    //      else m.toString()
    //    })
    //    if (regex.findFirstIn(result).isEmpty) Some(result) else None
  }

  /**
    * Retrieve the value of a reference from a given item.
    *
    * @param reference
    * @param item
    * @return
    */
  def processReference(reference: Literal, item: Item, encode: Boolean = false): Option[List[String]] = {
    if (encode) item.refer(reference.toString).map(list => list map Uri.encode)  else item.refer(reference.toString)
  }

  /**
    * Private util method for removing brackets from a template string.
    *
    * @param s
    * @return
    */
  private def removeBrackets(s: String): String = {
    s.replace("{", "")
      .replace("}", "")
  }

}
