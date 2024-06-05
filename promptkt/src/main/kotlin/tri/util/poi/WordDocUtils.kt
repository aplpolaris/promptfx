package tri.util.poi

import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File

/** Utilities for working with Word documents. */
object WordDocUtils {

    /** Read text from a DOC file. */
    fun readDoc(file: File) = WordExtractor(file.inputStream()).use { it.text }
    /** Read metadata from a DOC file. */
    fun readDocMetadata(file: File) = WordExtractor(file.inputStream()).use {
        it.summaryInformation.let {
            mapOf(
                "doc.title" to it.title,
                "doc.author" to it.author,
                "doc.subject" to it.subject,
                "doc.keywords" to it.keywords,
                "doc.comments" to it.comments,
                "doc.template" to it.template,
                "doc.lastAuthor" to it.lastAuthor,
                "doc.revNumber" to it.revNumber,
                "doc.createTime" to it.createDateTime,
                "doc.editTime" to it.editTime
            )
        }
    }

    /** Read text from a DOCX file. */
    fun readDocx(file: File) = XWPFWordExtractor(XWPFDocument(file.inputStream())).use { it.text }
    /** Read metadata from a DOCX file. */
    fun readDocxMetadata(file: File) = XWPFDocument(file.inputStream()).use {
        it.properties.coreProperties.let {
            mapOf(
                "docx.title" to it.title,
                "docx.author" to it.creator,
                "docx.subject" to it.subject,
                "docx.category" to it.category,
                "docx.keywords" to it.keywords,
                "docx.description" to it.description,
                "docx.created" to it.created,
                "docx.modified" to it.modified,
                "docx.modifiedBy" to it.lastModifiedByUser,
                "docx.contentStatus" to it.contentStatus,
                "docx.contentType" to it.contentType,
                "docx.version" to it.version,
                "docx.revision" to it.revision,
            )
        }
    }

}