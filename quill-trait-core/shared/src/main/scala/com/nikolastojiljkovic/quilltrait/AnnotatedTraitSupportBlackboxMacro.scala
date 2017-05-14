package com.nikolastojiljkovic.quilltrait

import com.nikolastojiljkovic.annotation.{ Embedded, Field, Table }
import io.getquill.util.Messages._

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox.{ Context => MacroContext }

@compileTimeOnly("AnnotatedTraitSupportBlackboxMacro is used during macro expansion only")
class AnnotatedTraitSupportBlackboxMacro(val c: MacroContext) {
  import c.universe._

  case class FieldInfo(columnName: String, embedded: Boolean = false)
  case class TableInfo(tableName: String)

  def loadSymbolAnnotations(s: Symbol): List[Annotation] = {
    // workaround for the scalac bug: https://issues.scala-lang.org/browse/SI-7424
    // force loading method's signature
    s.typeSignature
    // force loading all the annotations
    if (s.isMethod && s.asMethod.isAccessor) {
      s.asMethod.accessed.annotations.foreach(_.tree.tpe)
    }
    s.annotations.foreach(_.tree.tpe)

    s.annotations
  }

  def fieldAnnotationParsed(l: List[Annotation]): Option[FieldInfo] =
    l.flatMap(annotation => {
      annotation.tree.tpe match {
        case tpe if tpe =:= typeOf[Field] =>
          val field = c.eval[Field](c.Expr[Field](c.untypecheck(annotation.tree)))
          Some(FieldInfo(field.columnName))
        case tpe if tpe =:= typeOf[Embedded] =>
          Some(FieldInfo("", embedded = true))
        case _ =>
          None
      }
    }).headOption

  def tableAnnotationParsed(l: List[Annotation]): Option[TableInfo] =
    l.flatMap(annotation => {
      annotation.tree.tpe match {
        case tpe if tpe =:= typeOf[Table] =>
          val table = c.eval[Table](c.Expr[Table](c.untypecheck(annotation.tree)))
          Some(TableInfo(table.tableName))
        case _ =>
          None
      }
    }).headOption

  def materializeQueryMeta[C](implicit cTag: c.WeakTypeTag[C]): c.Tree = {
    val value = this.value("Decoder", cTag.tpe)
    q"""
      new ${c.prefix}.QueryMeta[$cTag] {
        val expand = ${expandQuery[C](value)}
        val extract = ${extract[C](value)}
      }
    """
  }

  def materializeUpdateMeta[C](implicit cTag: WeakTypeTag[C]): Tree =
    actionMeta[C](value("Encoder", cTag.tpe), "update")

  def materializeInsertMeta[C](implicit cTag: c.WeakTypeTag[C]): c.Tree =
    actionMeta[C](value("Encoder", cTag.tpe), "insert")

  def materializeSchemaMeta[C](implicit cTag: c.WeakTypeTag[C]): c.Tree = {
    val annotatedTypes = getAnnotatedType(cTag.tpe, optional = false)
    val tableName = getFlattenAnnotatedTypesTableName(annotatedTypes) match {
      case Some(t) =>
        t
      case _ =>
        annotatedTypes.headOption match {
          case Some(annotatedType) =>
            annotatedType.tpe.typeSymbol.name.decodedName.toString
          case _ =>
            c.fail("Could not evaluate table name.")
        }
    }

    val v = value("Encoder", cTag.tpe)
    val fields = flattenForSchemaMeta(v)

    q"""
      new ${c.prefix}.SchemaMeta[${cTag.tpe}] {
        val entity =
          ${c.prefix}.quote(
              ${c.prefix}.querySchema[${cTag.tpe}]($tableName, ..$fields)
          )
      }
    """
  }

  def flattenForSchemaMeta(value: Value): List[Tree] = {
    def getterToColumnFn(l: List[(Tree, Option[FieldInfo])]): List[Tree] =
      l.flatMap(p =>
        p._2.map(f =>
          q"(v => ${p._1} -> ${f.columnName})"))

    def flattenParams(base: Tree, params: List[List[Value]], optional: Boolean): List[Tree] =
      params.flatten.flatMap(apply(_, base, optional))

    def nest(tree: Tree, termO: Option[TermName], fieldInfo: Option[FieldInfo], isParentOptional: Boolean): (Tree, Option[FieldInfo]) =
      termO match {
        case Some(term) =>
          if (isParentOptional) {
            (q"$tree.map(_.$term)", fieldInfo)
          } else {
            (q"$tree.$term", fieldInfo)
          }
        case _ =>
          (tree, None)
      }

    def apply(value: Value, base: Tree, isParentOptional: Boolean): List[Tree] =
      value match {
        case Scalar(term, _, _, _, fieldInfo, _) =>
          getterToColumnFn(List(nest(base, term, fieldInfo, isParentOptional)))
        case Nested(term, _, params, optional, _) =>
          flattenParams(nest(base, term, None, isParentOptional)._1, params, optional)
      }

    apply(value, q"v", isParentOptional = false)
  }

  def actionMeta[T](value: Value, method: String)(implicit t: WeakTypeTag[T]) = {
    val assignments =
      flatten(q"v", value)
        .zip(flatten(q"value", value))
        .map {
          case (vTree, valueTree) =>
            q"(v: $t) => $vTree -> $valueTree"
        }
    c.untypecheck {
      q"""
        new ${c.prefix}.${TypeName(method.capitalize + "Meta")}[$t] {
          val expand =
            ${c.prefix}.quote((q: ${c.prefix}.EntityQuery[$t], value: $t) => q.${TermName(method)}(..$assignments))
        }
      """
    }
  }

  def expandQuery[T](value: Value)(implicit t: WeakTypeTag[T]) = {
    val elements = flatten(q"x", value)
    q"${c.prefix}.quote((q: ${c.prefix}.Query[$t]) => q.map(x => io.getquill.dsl.UnlimitedTuple(..$elements)))"
  }

  def extract[T](value: Value): Tree = {
    var index = -1

    def expandInTrait(value: Value): Tree = {
      value.term match {
        case Some(term) =>
          if (value.optional) {
            val innerTpe = if (is[Option[Any]](value.tpe)) {
              value.tpe.typeArgs.head
            } else {
              value.tpe
            }
            value match {
              case _: Scalar =>
                q"""val $term: Option[$innerTpe] = ${expand(value)}"""
              case _: Nested =>
                q"""val $term: Option[$innerTpe] = ${expand(value, parentOptional = true)}"""
            }
          } else {
            q"""val $term: ${value.tpe} = ${expand(value)}"""
          }
        case _ =>
          c.fail(s"Cannot expand $value")
      }
    }

    def expand(value: Value, parentOptional: Boolean = false): Tree =
      value match {

        case Scalar(_, tpe, decoder, optional, _, _) =>
          index += 1
          if (parentOptional) {
            if (optional)
              q"Some($decoder($index, row))"
            else
              q"implicitly[${c.prefix}.Decoder[Option[$tpe]]].apply($index, row)"
          } else {
            q"$decoder($index, row)"
          }

        case Nested(_, tpe, params, optional, _) =>
          if (parentOptional || optional) {
            val groups = params.map(_.map(expand(_, parentOptional = true)))
            val terms =
              groups.zipWithIndex.map {
                case (options, idx1) =>
                  options.indices.map { idx2 =>
                    TermName(s"o_${idx1}_$idx2")
                  }
              }
            groups.zipWithIndex.foldLeft(q"Some(new $tpe(...$terms))") {
              case (body, (options, idx1)) =>
                options.zipWithIndex.foldLeft(body) {
                  case (b, (option, idx2)) =>
                    val o = q"val ${TermName(s"o_${idx1}_$idx2")} = $EmptyTree"
                    q"$option.flatMap($o => $b)"
                }
            }
          } else {
            tpe.dealias.resultType match {
              case RefinedType(parents, _) =>
                val caseClassParams = params.map(_.filter(!_.isInTrait)).filter(_.nonEmpty)
                val traitParams = params.map(_.filter(_.isInTrait)).filter(_.nonEmpty)
                parents.headOption match {
                  case Some(caseClassTpe) if caseClassTpe.typeSymbol.asClass.isCaseClass =>
                    if (parents.tail.nonEmpty) {
                      q"""new $caseClassTpe(...${caseClassParams.map(_.map(expand(_)))}) with ..${parents.tail} {..${traitParams.flatMap(_.map(expandInTrait))}}"""
                    } else {
                      q"new $caseClassTpe(...${caseClassParams.map(_.map(expand(_)))})"
                    }
                  case _ =>
                    q"""new $tpe{..${traitParams.flatMap(_.map(expandInTrait))}}"""
                }
              case _ =>
                q"new $tpe(...${params.map(_.map(expand(_)))})"
            }
          }
      }

    q"(row: ${c.prefix}.ResultRow) => ${expand(value)}"
  }

  def flattenFieldInfos(value: Value): List[FieldInfo] = {
    def apply(params: List[List[Value]]): List[FieldInfo] =
      params.flatten.flatMap(flattenFieldInfos)

    value match {
      case Scalar(_, _, _, _, Some(fieldInfo), _) =>
        List(fieldInfo)
      case Nested(_, _, params, _, _) =>
        apply(params)
      case _ =>
        List()
    }
  }

  def validateDuplicateColumns(tpe: Type, value: Value): Value = {
    val fieldInfos = flattenFieldInfos(value)
    val filteredFieldInfos = fieldInfos.foldLeft[(List[FieldInfo], List[String])]((List(), List()))((s, f) => {
      if (s._1.exists(_.columnName == f.columnName)) {
        (s._1, s._2 :+ f.columnName)
      } else {
        (s._1 :+ f, s._2)
      }
    })
    if (filteredFieldInfos._2.nonEmpty) {
      c.fail(s"""Found duplicated column(s) ${filteredFieldInfos._2.distinct.mkString(", ")} in $tpe.""")
    }
    value
  }

  def flatten(base: Tree, value: Value): List[Tree] = {
    def nest(tree: Tree, term: Option[TermName]) =
      term match {
        case None    => tree
        case Some(t) => q"$tree.$t"
      }

    def apply(base: Tree, params: List[List[Value]]): List[Tree] =
      params.flatten.flatMap(flatten(base, _))

    value match {
      case Scalar(term, _, _, _, _, _) =>
        List(nest(base, term))
      case Nested(term, _, params, optional, _) =>
        if (optional)
          apply(q"v", params)
            .map(body => q"${nest(base, term)}.map(v => $body)")
        else
          apply(nest(base, term), params)
    }
  }

  sealed trait Value {
    val term: Option[TermName]
    val tpe: Type
    val isInTrait: Boolean
    val optional: Boolean
  }

  case class Nested(term: Option[TermName], tpe: Type, params: List[List[Value]], optional: Boolean, isInTrait: Boolean) extends Value

  case class Scalar(term: Option[TermName], tpe: Type, decoder: Tree, optional: Boolean, fieldInfo: Option[FieldInfo], isInTrait: Boolean) extends Value

  private def is[T](tpe: Type)(implicit t: TypeTag[T]) =
    tpe <:< t.tpe

  private def isTuple(tpe: Type) =
    tpe.typeSymbol.name.toString.startsWith("Tuple")

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.headOption

  case class AnnotatedType(tpe: Type, table: Option[TableInfo], fields: List[(Symbol, FieldInfo)], optional: Boolean)

  def getAnnotatedType(tpe: Type, optional: Boolean): List[AnnotatedType] = tpe.dealias.resultType match {
    case RefinedType(parents, _) =>
      parents.flatMap(getAnnotatedType(_, optional))
    case t @ _ if t.typeSymbol.isClass && t.typeSymbol.asClass.isCaseClass =>
      val fields = caseClassConstructor(t) match {
        case Some(constructor) =>
          constructor.paramLists.flatten.flatMap { param =>
            fieldAnnotationParsed(loadSymbolAnnotations(param)) match {
              case Some(fieldInfo) =>
                Some((
                  param,
                  fieldInfo
                ))
              case None =>
                None
            }
          }
        case None =>
          List()
      }
      List(AnnotatedType(t, tableAnnotationParsed(t.typeSymbol.annotations), fields, optional))
    case t @ _ if t.typeSymbol.isClass && t.typeSymbol.asClass.isTrait =>
      val fields = t.members.flatMap(member => {
        fieldAnnotationParsed(loadSymbolAnnotations(member)) match {
          case Some(fieldInfo) =>
            Some((
              member,
              fieldInfo
            ))
          case None =>
            None
        }
      }).toList
      // reverse the fields, for some reason in trait we get reverse order!?
      List(AnnotatedType(t, tableAnnotationParsed(t.typeSymbol.annotations), fields.reverse, optional))
    case t @ _ if is[Option[Any]](t) =>
      getAnnotatedType(t.typeArgs.head, optional = true)
    case _ =>
      c.fail(s"Found type '$tpe', but it is not a case class nor trait (not combination of these).")
  }

  def getFlattenAnnotatedTypesTableName(l: List[AnnotatedType]): Option[String] = {
    l.foldLeft[Option[String]](None)((o, t) => {
      (o, t.table.map(_.tableName)) match {
        case (Some(tableName), Some(tableNameC)) if tableName != tableNameC =>
          c.fail(s"Found refined type with different table names: $tableName and $tableNameC")
        case (Some(_), _) =>
          o
        case (_, p @ _) =>
          p

      }
    })
  }

  object OptionalTypecheck {
    def apply(tree: c.Tree): Option[c.Tree] =
      c.typecheck(tree, silent = true) match {
        case c.universe.EmptyTree => None
        case nonEmptyTree         => Some(nonEmptyTree)
      }
  }

  def value(encoding: String, tpe: Type, exclude: Tree*): Value = {

    def nest(tpe: Type, term: Option[TermName], depth: Int, isInTrait: Boolean): Nested = {
      val params = getAnnotatedType(tpe, optional = false).map(annotatedType => annotatedType.fields.map(t => {
        val member = t._1
        val fieldInfo = t._2
        apply(
          member.typeSignature.asSeenFrom(tpe, tpe.typeSymbol).resultType,
          Some(member.name.toTermName),
          nested = !isTuple(annotatedType.tpe),
          fieldInfo = Some(fieldInfo),
          depth + 1,
          annotatedType.tpe.typeSymbol.isClass && annotatedType.tpe.typeSymbol.asClass.isTrait
        )
      }))
      depth match {
        case 1 =>
          // filter out traits duplicated columns from base case class/trait
          case class FieldMeta(fieldInfo: FieldInfo, termName: TermName, tpe: Type)
          def merge(fields: List[FieldMeta], params: List[Value]): (List[FieldMeta], List[Value]) = {
            params.foldLeft[(List[FieldMeta], List[Value])]((fields, List()))((s, p) => {
              p match {
                case Scalar(Some(termName), sTpe, _, _, Some(fieldInfo), _) =>
                  val curFields = s._1
                  curFields.foreach(t => {
                    if (t.fieldInfo.columnName == fieldInfo.columnName) {
                      if (t.termName.decodedName.toString != termName.decodedName.toString) {
                        c.fail(s"""Column ${t.fieldInfo.columnName} is mapped to different val/defs in base case class and traits: ${t.termName.decodedName.toString} and ${termName.decodedName.toString}.""")
                      }
                      if (t.tpe.resultType != sTpe.resultType) {
                        c.fail(s"""Column ${t.fieldInfo.columnName} is mapped to conflicting types in base case class and traits: ${t.tpe} and $sTpe.""")
                      }
                      if (t.tpe.paramLists.nonEmpty || sTpe.paramLists.nonEmpty) {
                        c.fail(s"Column ${t.fieldInfo.columnName} is mapped to def with parameters.")
                      }
                    }
                  })
                  if (curFields.exists(_.fieldInfo.columnName == fieldInfo.columnName)) {
                    // skip trait column
                    s
                  } else {
                    (s._1 :+ FieldMeta(fieldInfo, termName, sTpe), s._2 :+ p)
                  }
                case _ =>
                  (s._1, s._2 :+ p)
              }
            })
          }

          val filteredParams = params.foldLeft[(List[FieldMeta], List[List[Value]])]((List(), List()))((s, l) => {
            val (newFields, ll) = merge(s._1, l)
            (newFields, s._2 :+ ll)
          })._2
          Nested(term, tpe, filteredParams, optional = false, isInTrait)
        case _ =>
          Nested(term, tpe, params, optional = false, isInTrait)
      }
    }

    def apply(tpe: Type, term: Option[TermName], nested: Boolean, fieldInfo: Option[FieldInfo] = None, depth: Int = 1, isInTrait: Boolean = false): Value = {
      OptionalTypecheck(q"implicitly[${c.prefix}.${TypeName(encoding)}[$tpe]]") match {
        case Some(enc) =>
          Scalar(term, tpe, enc, optional = is[Option[Any]](tpe), fieldInfo, isInTrait)
        case None =>
          def value(tpe: Type, forceEmbedded: Boolean, isInTrait: Boolean) =
            tpe match {
              case cTpe if forceEmbedded =>
                nest(cTpe, term, depth, isInTrait)

              case cTpe if !forceEmbedded && nested =>
                c.fail(
                  s"Can't expand nested value '$cTpe', please annotate it with `Embedded` " +
                    s"provide an implicit $encoding for it."
                )

              case cTpe =>
                nest(cTpe, term, depth, isInTrait)
            }

          val forceEmbedded = fieldInfo.exists(_.embedded)
          if (is[Option[Any]](tpe)) {
            value(tpe.typeArgs.head, forceEmbedded, isInTrait).copy(optional = true)
          } else {
            value(tpe, forceEmbedded, isInTrait)
          }
      }
    }

    def filterExcludes(value: Value) = {
      val paths =
        exclude.map {
          case f: Function =>
            def path(tree: Tree): List[TermName] =
              tree match {
                case q"$a.$b"                => path(a) :+ b
                case q"$a.map[$t]($b => $c)" => path(a) ++ path(c)
                case _                       => Nil
              }

            path(f.body)
        }

      def filter(value: Value, path: List[TermName] = Nil): Option[Value] =
        value match {
          case _ if paths.contains(path ++ value.term) =>
            None
          case Nested(term, t, params, optional, isInTrait) =>
            Some(Nested(term, t, params.map(_.flatMap(filter(_, path ++ term))), optional, isInTrait))
          case _ =>
            Some(value)
        }

      filter(value).getOrElse {
        c.fail("Can't exclude all entity properties")
      }
    }

    filterExcludes(
      validateDuplicateColumns(tpe, apply(tpe.resultType, term = None, nested = false))
    )
  }
}
