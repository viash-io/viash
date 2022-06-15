package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.CLIConf
import org.rogach.scallop.ScallopConfBase
import com.dataintuitive.viash.DocumentedSubcommand

object CLIExport {

  def printInformation(subconfigs: Seq[ScallopConfBase]) {
    subconfigs.map(
      _ match {
        case ds: DocumentedSubcommand => printInformation(ds)
        case u => Console.err.println(s"CLIExport: Unsupported type $u")
      }
    )
  }

  def printInformation(command: DocumentedSubcommand) {
    println(s"command name: ${command.getCommandNameAndAliases.mkString(" + ")}")
    for (o <- command.getOpts) {
      println(s"\n\tname: ${o.name} short: ${o.shortNames.mkString(",")} descr: ${o.descr} default: ${o.default}")
    }
    for (c <- command.getSubconfigs) {
      c match {
        case ds: DocumentedSubcommand => printInformation(ds)
        case _ => println(s"Unsupported type ${c.toString}")
      }
    }
  }


  def export() {
      val cli = new CLIConf(Nil)
      printInformation(cli.getSubconfigs)
  }
}
