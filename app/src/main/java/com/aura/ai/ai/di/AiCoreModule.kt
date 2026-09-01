package com.aura.ai.ai.di

import com.aura.ai.core.actions.ActionEngine
import com.aura.ai.core.actions.DefaultActionEngine
import com.aura.ai.core.intent.IntentRecognizer
import com.aura.ai.core.memory.ConversationBuffer
import com.aura.ai.core.memory.SlidingWindowConversationBuffer
import com.aura.ai.core.memory.classification.MemoryClassifier
import com.aura.ai.core.memory.classification.RuleBasedMemoryClassifier
import com.aura.ai.core.memory.context.ContextBuilder
import com.aura.ai.core.memory.context.DefaultContextBuilder
import com.aura.ai.core.memory.embedding.EmbeddingProvider
import com.aura.ai.core.memory.embedding.NoOpEmbeddingProvider
import com.aura.ai.core.memory.extraction.MemoryExtractor
import com.aura.ai.core.memory.extraction.RuleBasedMemoryExtractor
import com.aura.ai.core.memory.graph.DefaultKnowledgeGraph
import com.aura.ai.core.memory.graph.KnowledgeGraph
import com.aura.ai.core.memory.lifecycle.DefaultMemoryLifecycleManager
import com.aura.ai.core.memory.lifecycle.MemoryLifecycleManager
import com.aura.ai.core.memory.ranking.MemoryRanker
import com.aura.ai.core.memory.ranking.WeightedMemoryRanker
import com.aura.ai.core.memory.retrieval.DefaultMemoryRetriever
import com.aura.ai.core.memory.retrieval.MemoryRetriever
import com.aura.ai.core.memory.search.HybridSemanticSearch
import com.aura.ai.core.memory.search.SemanticSearch
import com.aura.ai.core.memory.store.InMemoryLongTermMemoryStore
import com.aura.ai.core.memory.store.InMemoryWorkingMemoryStore
import com.aura.ai.core.memory.store.LongTermMemoryStore
import com.aura.ai.core.memory.store.WorkingMemoryStore
import com.aura.ai.core.planner.Planner
import com.aura.ai.core.planner.TemplatePlanner
import com.aura.ai.core.plugin.intent.PluginAwareIntentRecognizer
import com.aura.ai.core.providers.AIProviderManager
import com.aura.ai.core.providers.DefaultAIProviderManager
import com.aura.ai.core.tools.DefaultToolRegistry
import com.aura.ai.core.tools.ToolRegistry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The one-to-one interface bindings for the AURA core-intelligence stack — the plain
 * [RepositoryModule]-style counterpart to [AiProvidersModule] (which needs multibinding) and
 * [AiToolsModule] (same). Every default implementation bound here is the one described in each
 * core module's own README as this phase's shipped default; swapping any of them for a smarter
 * implementation later (an LLM-backed [Planner] or [IntentRecognizer], say) is a one-line change
 * in this file and nowhere else.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiCoreModule {
    /** Phase 7: bound to the plugin-aware decorator rather than `KeywordIntentRecognizer`
     *  directly — see `com.aura.ai.core.plugin.intent.PluginAwareIntentRecognizer`'s own doc for
     *  why the built-in recognizer still always gets first attempt. */
    @Binds
    @Singleton
    abstract fun bindIntentRecognizer(impl: PluginAwareIntentRecognizer): IntentRecognizer

    @Binds
    @Singleton
    abstract fun bindPlanner(impl: TemplatePlanner): Planner

    @Binds
    @Singleton
    abstract fun bindConversationBuffer(impl: SlidingWindowConversationBuffer): ConversationBuffer

    @Binds
    @Singleton
    abstract fun bindToolRegistry(impl: DefaultToolRegistry): ToolRegistry

    @Binds
    @Singleton
    abstract fun bindActionEngine(impl: DefaultActionEngine): ActionEngine

    @Binds
    @Singleton
    abstract fun bindAIProviderManager(impl: DefaultAIProviderManager): AIProviderManager

    // --- Phase 4: Memory Intelligence -------------------------------------------------------

    @Binds
    @Singleton
    abstract fun bindLongTermMemoryStore(impl: InMemoryLongTermMemoryStore): LongTermMemoryStore

    @Binds
    @Singleton
    abstract fun bindWorkingMemoryStore(impl: InMemoryWorkingMemoryStore): WorkingMemoryStore

    @Binds
    @Singleton
    abstract fun bindMemoryClassifier(impl: RuleBasedMemoryClassifier): MemoryClassifier

    @Binds
    @Singleton
    abstract fun bindMemoryExtractor(impl: RuleBasedMemoryExtractor): MemoryExtractor

    @Binds
    @Singleton
    abstract fun bindMemoryRanker(impl: WeightedMemoryRanker): MemoryRanker

    @Binds
    @Singleton
    abstract fun bindMemoryRetriever(impl: DefaultMemoryRetriever): MemoryRetriever

    @Binds
    @Singleton
    abstract fun bindEmbeddingProvider(impl: NoOpEmbeddingProvider): EmbeddingProvider

    @Binds
    @Singleton
    abstract fun bindSemanticSearch(impl: HybridSemanticSearch): SemanticSearch

    @Binds
    @Singleton
    abstract fun bindContextBuilder(impl: DefaultContextBuilder): ContextBuilder

    @Binds
    @Singleton
    abstract fun bindMemoryLifecycleManager(impl: DefaultMemoryLifecycleManager): MemoryLifecycleManager

    // --- Version 1.0 Critical item 5: Knowledge Graph -------------------------------------

    @Binds
    @Singleton
    abstract fun bindKnowledgeGraph(impl: DefaultKnowledgeGraph): KnowledgeGraph
}
