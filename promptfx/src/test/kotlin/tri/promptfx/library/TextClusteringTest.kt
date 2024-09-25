/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.library

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.embedding.EmbeddingPrecision
import tri.ai.openai.OpenAiCompletionChat
import tri.ai.openai.OpenAiEmbeddingService
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.process.TextDocEmbeddings.putEmbeddingInfo
import tri.promptfx.library.TextClustering.generateClusterHierarchy
import tri.promptfx.library.TextClustering.generateClusterSummary
import tri.promptfx.library.TextClustering.generateClusters
import tri.promptfx.ui.chunk.TextChunkViewModel
import tri.promptfx.ui.chunk.asTextChunkViewModel
import tri.util.info
import tri.util.ml.AffinityClusterService

class TextClusteringTest {

    private val clusterService = AffinityClusterService()
    private val textCompletion = OpenAiCompletionChat()
    private val embeddingService = OpenAiEmbeddingService()

    @Disabled("Requires API key")
    @Test
    fun testGenerateClusterHierarchy() {
        val input = testInput()
        val clusters = runBlocking {
            clusterService.generateClusterHierarchy(
                input,
                ClusterSummaryType.CATEGORIES_AND_THEME,
                itemType = "Animals",
                categories = listOf("Land", "Sea", "Air", "Other"),
                sampleTheme = "Animals commonly found in the rainforest and known to climb trees.",
                textCompletion,
                embeddingService
            ) { _, _ -> }
        }
        println(clusters)
    }

    @Disabled("Requires API key")
    @Test
    fun testGenerateClusters() {
        val input = testInput()
        val clusters = runBlocking {
            clusterService.generateClusters(
                input.map { EmbeddingCluster(it) },
                ClusteringPrompt(
                    ClusterSummaryType.CATEGORIES_AND_THEME,
                    itemType = "Animals",
                    categories = listOf("Land", "Sea", "Air", "Other"),
                    sampleTheme = "Animals commonly found in the rainforest and known to climb trees."
                ),
                textCompletion,
                attempts = 2
            ) { _, it ->
                info<TextClustering>("Progress: ${"%.1f".format(it*100)}%")
            }
        }
        clusters.forEach {
            println(it)
        }
    }

    @Disabled("Requires API key")
    @Test
    fun testGenerateClusterSummary() {
        val input = testInput().map { EmbeddingCluster(it) }.take(10)
        println(input)
        val description = runBlocking {
            generateClusterSummary(
                input,
                ClusteringPrompt(
                    ClusterSummaryType.CATEGORIES_AND_THEME,
                    itemType = "Animals",
                    categories = listOf("Land", "Sea", "Air", "Other"),
                    sampleTheme = "Animals commonly found in the rainforest and known to climb trees."
                ),
                textCompletion,
                attempts = 2
            )
        }
        println(description)
    }

    private fun testInput(): List<TextChunkViewModel> {
        val embeddings = runBlocking { embeddingService.calculateEmbedding(ANIMAL_NAMES) }
        return ANIMAL_NAMES.mapIndexed { i, s ->
            val chunk = TextChunkRaw(s)
            chunk.putEmbeddingInfo(embeddingService.modelId, embeddings[i], EmbeddingPrecision.FULL)
            chunk.asTextChunkViewModel(null, embeddingService.modelId)
        }
    }

    private val ANIMAL_NAMES = """
        Aardvark
        Aardwolf
        Armadillo
        Arrow crab
        Asp
        Ass (donkey)
        Baboon
        Badger
        Boar
        Bobcat
        Bobolink
        Canid
        Cape buffalo
        Capybara
        Cardinal
        Caribou
        Carp
        Cat
        Catshark
        Caterpillar
        Coral
        Cougar
        Cow
        Coyote
        Crab
        Crane
        Crane fly
        Crawdad
        Crayfish
        Cricket
        Crocodile
        Crow
        Cuckoo
        Cicada
        Damselfly
        Deer
        Dingo
        Dinosaur
        Dog
        Ermine
        Falcon
        Ferret
        Finch
        Firefly
        Fish
        Flamingo
        Flea
        Fly
        Flyingfish
        Fowl
        Fox
        Frog
        Fruit bat
        Gamefowl
        Galliform
        Guanaco
        Guineafowl
        Guinea pig
        Kangaroo
        Kangaroo mouse
        Kangaroo rat
        Kingfisher
        Kite
        Kiwi
        Koala
        Koi
        Locust
        Loon
        Louse        
        Moth
        Mountain goat
        Mouse
        Mule
        Muskox
        Narwhal
        Newt
        New World quail
        Nightingale
        Ocelot
        Octopus
        Old World quail
        Opossum
        Orangutan
        Orca
        Ostrich
        Otter
        Owl
        Ox
        Panda
        Panther
        Panthera hybrid
        Parakeet
        Parrot
        Parrotfish
        Partridge
        Peacock
        Peafowl
        Pelican
        Penguin
        Perch
        Peregrine falcon
        Pheasant
        Pig
        Pigeon
        Pike
        Pilot whale
        Pinniped
        Piranha
        Planarian
        Platypus
        Polar bear
        Pony
        Porcupine
        Porpoise
        Portuguese man o' war
        Possum
        Prairie dog
        Prawn
        Praying mantis
        Primate
        Ptarmigan
        Puffin
        Puma
        Python
        Quail
        Salamander
        Salmon
        Sawfish
        Scale insect
        Scallop
        Scorpion
        Seahorse
        Sea lion
        Sea slug
        Sea snail
        Shark
        Sheep
        Shrew
        Shrimp
        Silkworm
        Silverfish
        Skink
        Skunk
        Sloth
        Slug
        Smelt
        Snail
        Snake
        Snipe
        Snow leopard
        Swordtail
        Tahr
        Takin
        Tapir
        Tarantula
        Tarsier
        Tasmanian devil
        Termite
        Tern
        Thrush
        Tick
        Tiger
        Tiger shark
        Tiglon
        Toad
        Tortoise
        Toucan
        Trapdoor spider
        Tree frog
        Trout
        Tuna
        Turkey
        Turtle
        Tyrannosaurus
        Urial
        Vampire bat
        Vampire squid
        Vicuna
        Viper
        Vole
        Vulture
        Wallaby
        Walrus
        Wasp
        Warbler
        Water Boa
        Water buffalo
        Zebra
        Zebra finch
    """.trimIndent().split("\n").map { it.trim() }

}
