/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.util.ml

import com.clust4j.algo.AffinityPropagation
import com.clust4j.algo.AffinityPropagationParameters
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import tri.util.info

/** Clustering service using Affinity Propagation. */
class AffinityClusterService : ClusterService {
    override fun <T> cluster(items: List<T>, op: (T) -> List<Double>): List<List<T>> {
        val indexedItems = items.map { op(it).toDoubleArray() }
        val obsMatrix = Array2DRowRealMatrix(indexedItems.toTypedArray())
        try {
            val aff: AffinityPropagation = AffinityPropagationParameters()
                .fitNewModel(obsMatrix)
            val labels = aff.labels
            val itemClusters = items.withIndex().groupBy { labels[it.index] }
                .mapValues { it.value.map { it.value } }
            info<AffinityClusterService>("Computed ${aff.numberOfIdentifiedClusters} clusters of ${items.size} items")
            info<AffinityClusterService>("  > cluster sizes: ${itemClusters.values.map { it.size }}")
            return itemClusters.values.toList()
        } catch (ex: Exception) {
            ex.printStackTrace()
            return listOf()
        }
    }
}
