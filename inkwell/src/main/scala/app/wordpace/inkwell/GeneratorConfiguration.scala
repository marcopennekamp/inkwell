package app.wordpace.inkwell

import java.nio.file.Path

import app.wordpace.inkwell.generator._
import app.wordpace.inkwell.schema._

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
    * The target folder where the root folder of the base package will be placed.
    *
    * For example, assume the base package is `com.example`, we have one compilation unit with a
    * name `schema.Schema`, and the target folder is `target/scala-2.12/src_managed`. [[Generator]]
    * will write the compilation unit to the file `target/scala-2.12/src_managed/com/example/schema/Schema.scala`.
    */
  def targetFolder: Path

  /**
    * The base package of all compilation units, which is basically concatenated with each unit's name.
    */
  def basePackage: String

  /**
    * A Path to your scalafmt configuration file or None if no code formatting is desired.
    */
  def scalafmtConfig: Option[Path]

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
    * The type emitter can be overridden to change how types, type names and/or column types are emitted.
    */
  def typeEmitter: TypeEmitter

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
    * Selects the property emitter based on the given column (and possibly the table and even schema).
    */
  def selectPropertyEmitter(column: Column): PropertyEmitter
}

case class DefaultGeneratorConfiguration(
  override val db: DatabaseConfiguration,
  override val sourceSchema: String,
  override val targetFolder: Path,
  override val basePackage: String,
) extends GeneratorConfiguration {
  /**
    * A map of custom JDBC to [[TypeReference]] mappings, used by the [[DefaultTypeResolver]]. Note that this
    * map is not (automatically) used if you override [[typeResolver]].
    */
  def customTypes: Map[String, TypeReference] = Map.empty

  /**
    * The inheritance map is used by [[DefaultModelEmitter]] to provide support for model supertypes. You can
    * override this value to provide your own inheritance rules. Note that this map is not (automatically) used
    * if you override [[selectModelEmitter]].
    */
  def inheritances: Inheritances = Inheritances.empty

  override def scalafmtConfig: Option[Path] = None
  override def ignoredTables: Set[String] = Set.empty
  override def namingStrategy: NamingStrategy = SnakeCaseToCamelCase
  override def imports: Set[Import] = Set.empty

  override lazy val schemaReader: SchemaReader = new DefaultSchemaReader(this)
  override lazy val typeResolver: TypeResolver = new DefaultTypeResolver(customTypes)
  override lazy val typeEmitter: TypeEmitter = new ImportSimplifyingTypeEmitter(imports)

  override def selectSchemaEmitter(schema: Schema): SchemaEmitter = new SingleFileSchemaEmitter(this, schema, "Schema")
  override def selectModelEmitter(table: Table): ModelEmitter = new DefaultModelEmitter(this, inheritances, table)
  override def selectCompanionEmitter(table: Table): CompanionEmitter = new DefaultCompanionEmitter(this, table)
  override def selectPropertyEmitter(column: Column): PropertyEmitter = new DefaultPropertyEmitter(this, column)
}
