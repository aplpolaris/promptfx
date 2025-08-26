/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.util.io.pdf

import org.apache.pdfbox.contentstream.PDFStreamEngine
import org.apache.pdfbox.contentstream.operator.DrawObject
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.contentstream.operator.state.Concatenate
import org.apache.pdfbox.contentstream.operator.state.Restore
import org.apache.pdfbox.contentstream.operator.state.Save
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters
import org.apache.pdfbox.contentstream.operator.state.SetMatrix
import org.apache.pdfbox.cos.COSBase
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.IOException

/** Gets image locations. */
class PdfImageFinder : PDFStreamEngine() {

    val result = mutableListOf<PdfImageInfo>()

    init {
        addOperator(Concatenate(this))
        addOperator(DrawObject(this))
        addOperator(SetGraphicsStateParameters(this))
        addOperator(Save(this))
        addOperator(Restore(this))
        addOperator(SetMatrix(this))
    }

    @Throws(IOException::class)
    override fun processOperator(operator: Operator, operands: List<COSBase>) {
        val operation = operator.name
        if ("Do" == operation) {
            val objectName = operands[0] as COSName
            val obj = resources.getXObject(objectName)
            if (obj is PDImageXObject) {
                val ctmNew = graphicsState.currentTransformationMatrix
                result.add(PdfImageInfo(
                    objectName.name,
                    Rectangle2D.Float(ctmNew.translateX, ctmNew.translateY, ctmNew.scalingFactorX, ctmNew.scalingFactorY),
                    null,
                    obj.width, obj.height
                ))
            }
        } else {
            super.processOperator(operator, operands)
        }
    }
}

/** Store PDF image information. */
data class PdfImageInfo(val name: String, val bounds: Rectangle2D, val image: BufferedImage?, val width: Int, val height: Int) {
    fun withImage(img: BufferedImage?) = copy(image = img)
}
