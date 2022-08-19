package io.viash

package object schemas {
  import scala.annotation.meta._

  @getter @setter @beanGetter @beanSetter @field
  class since(since: String) extends scala.annotation.StaticAnnotation

  @getter @setter @beanGetter @beanSetter @field
  class example(example: String, format: String) extends scala.annotation.StaticAnnotation

  @getter @setter @beanGetter @beanSetter @field
  class exampleWithDescription(example: String, format: String, description: String) extends scala.annotation.StaticAnnotation

  @getter @setter @beanGetter @beanSetter @field
  class description(example: String) extends scala.annotation.StaticAnnotation

  @getter @setter @beanGetter @beanSetter @field
  class deprecated(message: String = "", since: String = "") extends scala.annotation.StaticAnnotation

  @getter @setter @beanGetter @beanSetter @field
  class removed(message: String = "", since: String = "") extends scala.annotation.StaticAnnotation
}
