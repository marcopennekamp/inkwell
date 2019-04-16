package com.github.choppythelumberjack.trivialgen

import java.nio.file.Path

import scala.reflect.runtime.universe.Type

import com.github.choppythelumberjack.trivialgen.generator._
import com.github.choppythelumberjack.trivialgen.schema._

case class DatabaseConfiguration(
  url: String,
  username: String,
  password: String,
)

/**
  * Basic and advanced configuration options that can be modified without extending the generator class.
  *
  * In most cases, extend [[DefaultGeneratorConfiguration]] instead of this trait directly.
  */
trait GeneratorConfiguration {
  /**
    * The [[DatabaseConfiguration]] used to access your local database.
    */
  def db: DatabaseConfiguration

  /**
    * The name of the schema used as the basis for code generation.
    */
  def sourceSchema: String

  /**
    * The target folder or <b>extensionless</b> target file (depending on your [[SchemaEmitter]]).
    *
    * For example, [[SingleFileSchemaEmitter]] assumes the target to be a file.
    */
  def target: Path

  /**
    * The base package of the target.
    */
  def basePackage: String

  /**
    * Names of all tables that should be ignored during code generation.
    */
  def ignoredTables: Set[String]

  /**
    * The naming strategy turns SQL names into Scala names for tables and columns (classes and attributes).
    * You can use one of the provided naming strategies or implement your own.
    */
  def namingStrategy: NamingStrategy

  /**
    * A set of imported classes and packages which will be accessible by simple name in the generated code.
    */
  def imports: Set[Import]

  /**
    * The schema reader fetches the schema from the database and transforms it into a schema model. You generally don't
    * need to override this, but the option is there just in case.
    */
  def schemaReader: SchemaReader

  /**
    * The type resolver translates JDBC types to Scala types.
    */
  def typeResolver: TypeResolver

  /**
    * The raw type builder can be overridden to change how type names are turned to strings globally, instead
    * of overriding a specific emitter's rawType definition.
    */
  def rawTypeBuilder: RawTypeBuilder

  /**
    * Selects the schema emitter based on the given schema.
    */
  def selectSchemaEmitter(schema: Schema): SchemaEmitter

  /**
    * Selects the model emitter based on the given table (and possibly the schema).
    */
  def selectModelEmitter(table: Table): ModelEmitter

  /**
    * Selects the companion emitter based on the given table (and possibly the schema).
    */
  def selectCompanionEmitter(table: Table): CompanionEmitter

  /**
    * Selects the property emitter based on the given column (and possibly table and even schema).
    */
  def selectPropertyEmitter(column: Column): PropertyEmitter
}

case class DefaultGeneratorConfiguration(
  override val db: DatabaseConfiguration,
  override val sourceSchema: String,
  override val target: Path,
  override val basePackage: String,
) extends GeneratorConfiguration {

  /**
    * A map of custom JDBC to Scala type mappings, used by the [[DefaultTypeResolver]]. Note that this
    * map is not used if you override [[typeResolver]].
    */
  def customTypes: Map[String, Type] = Map.empty

  /**
    * The schema inheritance map is used by [[DefaultModelEmitter]] to provide support for model supertypes.
    * You can override this value to provide your own inheritance rules.
    */
  def inheritances: SchemaInheritances = SchemaInheritances.empty

  override def ignoredTables: Set[String] = Set.empty
  override def namingStrategy: NamingStrategy = SnakeCaseToCamelCase
  override def imports: Set[Import] = Set.empty

  override lazy val schemaReader: SchemaReader = new DefaultSchemaReader(this)
  override lazy val typeResolver: TypeResolver = new DefaultTypeResolver(customTypes)
  override lazy val rawTypeBuilder: RawTypeBuilder = new ImportSimplifyingRawTypeBuilder(imports)

  override def selectSchemaEmitter(schema: Schema): SchemaEmitter = new SingleFileSchemaEmitter(this, schema)
  override def selectModelEmitter(table: Table): ModelEmitter = new DefaultModelEmitter(this, inheritances, table)
  override def selectCompanionEmitter(table: Table): CompanionEmitter = new DefaultCompanionEmitter(this, table)
  override def selectPropertyEmitter(column: Column): PropertyEmitter = new DefaultPropertyEmitter(this, column)
}
