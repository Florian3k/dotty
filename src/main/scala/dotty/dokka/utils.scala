package dotty.dokka

import org.jetbrains.dokka.model.properties._
import org.jetbrains.dokka.base.signatures._
import org.jetbrains.dokka.model._
import org.jetbrains.dokka.pages._
import org.jetbrains.dokka.links._
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.utilities.DokkaLogger
import collection.JavaConverters._
import org.jetbrains.dokka.base.translators.documentables._
import org.jetbrains.dokka.model.properties.PropertyContainer
import dokka.java.api._
import java.util.function.Consumer
import kotlin.jvm.functions.Function2
import java.util.{List => JList, Set => JSet, Map => JMap}
import org.jetbrains.dokka.DokkaConfiguration$DokkaSourceSet

extension  on[T, V] (a: WithExtraProperties[T]):
  def get(key: ExtraProperty.Key[_ >: T, V]): V = a.getExtra().getMap().get(key).asInstanceOf[V]

extension on[V] (map: JMap[DokkaConfiguration$DokkaSourceSet, V]):
    def defaultValue: V = map.values.asScala.toSeq(0)

extension on(builder: PageContentBuilder$DocumentableContentBuilder):

    def addText(str: String) = builder.text(str, ContentKind.Main, builder.getMainSourcesetData, builder.getMainStyles, builder.getMainExtra) 
    
    def addList[T](
        elements: JList[T],
        prefix: String = "",
        suffix: String = "",
        separator: String = ", "
    )(op: T => Unit): Unit = 
        val lambda: Function2[Any, Any, kotlin.Unit] = new Function2[Any,Any, kotlin.Unit]:
            def invoke(a: Any, b: Any) = 
                op(b.asInstanceOf[T])
                kotlin.Unit.INSTANCE

        builder.list(elements, prefix, suffix, separator, builder.getMainSourcesetData, lambda)

    def addLink(
            text: String,
            address: DRI,
            kind: Kind = ContentKind.Main,
            sourceSets: JSet[DokkaConfiguration$DokkaSourceSet] = builder.getMainSourcesetData,
            styles: JSet[Style] = builder.getMainStyles,
            extra: PropertyContainer[ContentNode]= builder.getMainExtra) =
            builder.link(text, address, kind, sourceSets, styles, extra)

object JList:
    def apply[T](elem: T): JList[T] = List(elem).asJava
    def apply[T]() = List[T]().asJava

object JSet:
    def apply[T](elem: T): JSet[T] = Set(elem).asJava
    def apply[T]() = Set[T]().asJava

def modifyContentGroup(originalContentNodeWithParents: Seq[ContentGroup], modifiedContentNode: ContentGroup): ContentGroup =
    originalContentNodeWithParents match {
        case head :: tail => tail match {
            case tailHead :: tailTail =>
                val newChildren = tailHead.getChildren.asScala.map(c => if c != head then c else modifiedContentNode)
                modifyContentGroup(
                    tailTail,
                    tailHead.copy(
                        newChildren.asJava,
                        tailHead.getDci,
                        tailHead.getSourceSets,
                        tailHead.getStyle,
                        tailHead.getExtra
                    )
                )
            case _ => head
        }
        case _ => modifiedContentNode
    }

def getContentGroupWithParents(root: ContentGroup, condition: ContentGroup => Boolean): Seq[ContentGroup] = {
    def getFirstMatch(list: List[ContentNode]): Seq[ContentGroup] = list match {
        case head :: tail => head match {
            case g: ContentGroup => 
                val res = getContentGroupWithParents(g, condition)
                if(!res.isEmpty) res
                else getFirstMatch(tail)
            case _ => getFirstMatch(tail)
        }
            
        case _ => Seq()
    }
    if(condition(root)) Seq(root)
    else {
        val res = getFirstMatch(root.getChildren.asScala.toList)
        if(!res.isEmpty) res ++ Seq(root)
        else Seq()
    }
}
