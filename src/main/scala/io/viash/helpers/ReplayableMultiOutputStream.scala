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
}
