# Inkwell

Inkwell is a highly customisable code generator, which generates Scala case classes from a database schema. It is built for and tested with [Quill](https://github.com/getquill/quill), but can be potentially used in other cases.

What makes Inkwell special? Check this out:

- **It's highly customisable:** Generate exactly the code you need. Inkwell is *highly* customisable. You can adjust every aspect of schema code generation with varying degrees of ease.   
- **It's easy to use:** Inkwell provides opinionated default implementations to help you get started quickly. You can pick and choose the components you need or write your own. It's also very easy to extend Inkwell's default implementations.
- **It's well documented:** I know how frustrating bad documentation (or none at all!) can be, so I took my time to carefully document everything important. Just check out the source code! Is anything unclear? Go raise an issue!
- **It's open for more:** I'd love to enrich Inkwell with your feedback, so get issue tracking!

The name Inkwell is a play on Quill. To write with a quill, you first need to dip it in ink. An inkwell is a quick and easy method of inking your quill *consistently*. Inkwell provides consistently up-to-date "ink" for Quill by generating all the classes you need to get started with your queries.


## Installation

[![Maven][mavenImage]][mavenUrl]

Add Inkwell to your project's library dependencies:

```
libraryDependencies += "app.wordpace" %% "inkwell" % "0.2.0"
```

Note that Inkwell works with `scala.reflect.runtime.universe.Type` and `typeOf`, because `ClassTag` doesn't contain any information about type arguments. To use `Type` and `typeOf`, you may have to add the following dependency to your build:

```scala
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
```


## Getting Started

Inkwell must be invoked from Scala code. There is no standalone executable or command line interface. Hence, you will need to set up a [multi-project build](https://www.scala-sbt.org/1.x/docs/Multi-Project.html) in SBT with a main project and a code generation project, which your main project should depend on. Your Inkwell generator will be placed in the code generation project, but *invoked* as a [code generation task](https://www.scala-sbt.org/1.0/docs/Howto-Generating-Files.html) in the settings of the main project. Check out Inkwell's own [build.sbt](https://github.com/marcopennekamp/inkwell/blob/master/build.sbt) as an example.

If you get a `java.sql.SQLException: No suitable driver found` exception, you may have to initialise your database driver manually:

```
Class.forName(databaseDriver)
// e.g. Class.forName("org.postgresql.Driver")
```

Once you have set up the code generation framework, you need to configure and invoke the Inkwell code generator. A minimal setup with a `FileGenerator` looks like this:

```scala
val config: DefaultGeneratorConfiguration = new DefaultGeneratorConfiguration(
  DatabaseConfiguration(url, username, password),
  // Check your database's documentation for the default schema name. It is "public" in Postgres.
  sourceSchema = "public",
  // The src folder that all files get generated to.
  targetFolder = Paths.get(outputDir),
  // The base package of every generated file and conversely all case classes/objects.
  basePackage = "com.example.schema",
) { configSelf =>

}
new FileGenerator(config).generate()
```

**And now you can start overriding.** The following code samples would be placed between the curly brackets of the basic example above.

Let's say you want to **ignore a table** `schema_version` (used by Flyway):

```scala
override def ignoredTables: Set[String] = Set("schema_version")
```

Or maybe **import some types:**

```scala
override val imports: Set[Import] = Set(
  Import.Wildcard("java.time"),
  Import.Entity("play.api.libs.json.Json"),
  Import.Entity("core.Id"),
  Import.Entity(typeOf[Identity]),
)
```

**Extend a case class** generated from a table `account`:

```scala
override def inheritances: Inheritances = Inheritances(Map(
  "Account" -> Seq(typeOf[Identity]),
))
```

**Map custom types.** `TypeReference.conversions` allow you to create a type reference from a `String` or `Type` without any boilerplate code.

```scala
import TypeReference.conversions._

override def customTypes: Map[String, TypeReference] = Map(
  "my_enum_1" -> typeOf[MyEnum1],
  "my_enum_2" -> "com.example.enums.MyEnum2",
)
```

Generate an `Id[A]` type (e.g. `id: Id[Account]` for `Account`) for **foreign key and primary key** properties:

```scala
override def createProperty(column: Column, model: Model): Property = {
  new KeyAsIdProperty(column, model, this) {
    override protected def id(modelType: TypeReference): TypeReference = {
      NamedTypeReference("app.wordpace.backend.core.Id", Seq(modelType))
    }
  }
}
```

And *of course* you can **just ******* generate some code:**

```scala
override lazy val companionEmitter: CompanionEmitter = {
  new DefaultCompanionEmitter(this) {
    override protected def innerCode(model: Model): String = {
      s"""implicit val reads = Json.reads[${model.simpleName}]
         |implicit val writes = Json.writes[${model.simpleName}]
         |def tupled = (${model.simpleName}.apply _).tupled""".stripMargin
    }
  }
}
```

The options presented above are just a small subset of what you can do with Inkwell. The next section gives you an overview of Inkwell's concepts and components. When in doubt, **read the source documentation and code**. You'll find it quite approachable.


## Concepts and Components

This is a high-level overview of the different kinds of concepts and components in Inkwell. For more in-depth information, please consult the linked source files.

##### Concepts

- A [`GeneratorConfiguration`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/GeneratorConfiguration.scala) is used by [`Generator`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/Generator.scala) to get or create any kind of component set in the configuration. Thus, any component you want to extend or swap out should be overridden in `GeneratorConfiguration`, as has been done in the examples above.
- [JdbcModel](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/schema/JdbcModel.scala) is a direct representation for the data returned by JDBC. It is confined to `SchemaReader` and otherwise only used by `TypeResolver`.
- [SchemaModel](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/schema/SchemaModel.scala) is a friendly representation of the database schema. It is the output of the schema reader and used as the first intermediate representation of the schema.
- [`CompilationUnit`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/CompilationUnit.scala), [`Model`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/Model.scala) and [`Property`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/Property.scala) are representations of the specific parts of code that are emitted by Inkwell. A `CompilationUnit` collects all models to be emitted to the same file and the required imports, a `Model` represents one table that will be emitted to both a case class and potentially a companion object, and a `Property` represents a single property of a single case class.
  - `KeyAsIdProperty` sets `dataType` to an ID type for primary and foreign key columns. You have to build the type reference yourself, as seen in the example above where the `id` method is overridden.
- A [`TypeReference`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/TypeReference.scala) points to the name and the type arguments of a type. While you should use `ScalaTypeReference` if possible, notably, types can also be represented by `NamedTypeReference`, i.e. by their full name and a list of `TypeReference` type arguments. Thus, you can generate types which do not yet exist at generator runtime. See the documentation of `NamedTypeReference` for an example.
- An [`Import`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/Import.scala) is either an "entity" import such as `import scala.reflect.runtime.universe.Type` or a "wildcard" import such as `import scala.reflect.runtime.universe._`. You can configure imports in `GeneratorConfiguration`.

##### Components

All components have default implementations contained in the same file. We don't specifically list them here. However, we do list "advanced" components such as `ImportSimplifyingTypeEmitter`.

- [`SchemaReader`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/schema/SchemaReader.scala) reads a database schema via JDBC into Inkwell's schema model. You probably don't want to override this. If the model is missing some information you need, create an issue.
- [`TypeResolver`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/schema/TypeResolver.scala) maps a `JdbcColumnMeta` to a `TypeReference`. You can use type resolver to handle custom JDBC types.
- [`NamingStrategy`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/NamingStrategy.scala) translates SQL names to Scala names. You can override this to affect generated type and property names.
- [`SchemaSlicer`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/SchemaSlicer.scala) distributes all models from the given [`ModelRepository`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/ModelRepository.scala) to a set of compilation units. Notably, the schema slicer can be used to decide which model should become part of which file.
  - `SingleUnitSchemaSlicer` puts all models into one compilation unit.
  - `PartitioningSchemaSlicer` can be used to distribute models to different sub-packages based on your own configuration.
- [`TypeEmitter`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/TypeEmitter.scala) turns type references into strings. The emitter can see which compilation unit a given type is in and use information from the unit (such as its package declaration).
  - `ImportSimplifyingTypeEmitter` simplifies full type names based on standard (e.g. `scala.lang._`) and configured imports.
- [`CompilationUnitEmitter`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/CompilationUnitEmitter.scala) emits the code for a given compilation unit.
- [`ModelEmitter`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/ModelEmitter.scala) emits a case class for a given `Model`.
- [`CompanionEmitter`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/CompanionEmitter.scala) emits a companion object for a given `Model`. The `DefaultCompanionEmitter` should be extended if you want to emit a companion object at all: Default companion objects are only emitted if their inner code is not empty.
- [`PropertyEmitter`](https://github.com/marcopennekamp/inkwell/blob/master/inkwell/src/main/scala/app/wordpace/inkwell/generator/PropertyEmitter.scala) emits a single property of a case class for a given `Property`.


## State of the Project

Inkwell is in early stages. I have just started using it in my own projects. However, since it's a code generator, it's very easy to try it out (and rip it out if the need arises), so give it a go!

Inkwell has been tested with:
 
- **Databases** – PostgreSQL 10
- **Libraries** – Quill 3.1.0 

You can help by testing whether Inkwell works with other databases.

Thanks to [@deusaquilus](https://github.com/deusaquilus) and [@olafurpg](https://github.com/olafurpg) for prior work!


## Changelog

##### 0.2.0

This version is a very extensive refactoring, as you can read below. This is the first step in my ongoing efforts to improve the design and structure of Inkwell.

- Split `Model` and `Property` from their respective emitters, to separate the emitter from the additional data processing that can now be done with `Model` and `Property`. This is a much cleaner design, albeit a little bit more complex.
- Split `SchemaEmitter` into, on the one hand, `CompilationUnit` and `CompilationUnitEmitter`, which hold info about each code unit and emit it, and `SchemaSlicer`, which distributes each model to a compilation unit. Previously, `SchemaEmitter` had both of these tasks, which became messy.
- Allow `TypeEmitter` to access the `CompilationUnit` a given type should be emitted to, which can be used, for example, to glean the imports specific to said compilation unit.
- Move Id-type resolution based on database keys from `TypeEmitter` to `Property`. There was previously a design oversight which meant that the Id type wasn't treated as a `TypeReference`.
- `DefaultCompilationUnitEmitter` now sorts imports alphabetically by default.

##### 0.1.1 (unpublished)

- Fix missing imports for `PartitioningSchemaEmitter`. The emitter now automatically imports all other partitions (including the unpartitioned set), so that references to classes in other partitions can be made with a simple name. 

##### 0.1.0

- Initial Release

[mavenImage]: https://img.shields.io/maven-central/v/app.wordpace/inkwell_2.12.svg
[mavenUrl]: https://search.maven.org/artifact/app.wordpace/inkwell_2.12/
