/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.helpers

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import scala.collection.mutable.ListBuffer

class ReplayableOutputStream(index: Int, buffer: ListBuffer[(Int, String)]) extends OutputStream {

  override def write(b: Int): Unit = {
    buffer += ((index, b.toChar.toString))
  }

  override def write(b: Array[Byte]): Unit = {
    buffer += ((index, new String(b)))
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    buffer += ((index, new String(b, off, len)))
  }

  override def flush(): Unit = {
  }

  override def close(): Unit = {
  }
  
}

class ReplayableMultiOutputStream {

  val buffer = ListBuffer[(Int, String)]()
  val outputStreams = ListBuffer[ReplayableOutputStream]()
  val callbacks = ListBuffer[(String) => Unit]()

  def getOutputStream(callback: (String) => Unit): OutputStream = {
    val index = outputStreams.length
    val newStream = new ReplayableOutputStream(index, buffer)
    outputStreams += newStream
    callbacks += callback
    newStream
  }

  def replay(): Unit = {
    buffer.foreach { case (index, data) =>
      callbacks(index)(data)
    }
  }

  override def toString(): String = {
    buffer.map(_._2).mkString
  }
}
