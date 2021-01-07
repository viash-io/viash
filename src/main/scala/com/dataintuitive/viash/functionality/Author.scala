package com.dataintuitive.viash.functionality

case class Author(
  name: String,
  email: Option[String] = None,
  roles: List[String] = Nil,
  props: Map[String, String] = Map.empty[String, String]
) {
  override def toString: String = {
    name +
      email.map(" <" + _ + ">").getOrElse("") +
      { if (roles.isEmpty) "" else " (" + roles.mkString(", ") + ")"} +
      { if (props.isEmpty) "" else " {" + props.map(a => a._1 + ": " + a._2).mkString(", ") + "}"}
  }
}
