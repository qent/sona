package io.qent.sona.core.state

import io.qent.sona.core.presets.LlmProvider
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.presets.PresetsStateFlow
import io.qent.sona.core.state.interactors.PresetsListStateInteractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakePresetsRepository(var data: Presets = Presets(0, emptyList())) : PresetsRepository {
    override suspend fun load(): Presets = data
    override suspend fun save(presets: Presets) { data = presets }
}

class PresetsListStateInteractorTest {
    @Test
    fun addPresetUpdatesRepository() = runBlocking {
        val provider = LlmProvider("p", "e", emptyList())
        val repo = FakePresetsRepository(Presets(0, emptyList()))
        val flow = PresetsStateFlow(repo)
        val interactor = PresetsListStateInteractor(flow)
        interactor.load()
        interactor.addPreset(Preset("n", provider, "e", "m", "k"))
        assertEquals(1, repo.data.presets.size)
        assertEquals(0, repo.data.active)
    }

    @Test
    fun selectPresetPersistsActive() = runBlocking {
        val provider = LlmProvider("p", "e", emptyList())
        val p1 = Preset("a", provider, "e", "m", "k")
        val p2 = Preset("b", provider, "e", "m", "k")
        val repo = FakePresetsRepository(Presets(0, listOf(p1, p2)))
        val flow = PresetsStateFlow(repo)
        val interactor = PresetsListStateInteractor(flow)
        interactor.load()
        interactor.selectPreset(1)
        assertEquals(1, repo.data.active)
    }

    @Test
    fun updatePresetUpdatesRepository() = runBlocking {
        val provider = LlmProvider("p", "e", emptyList())
        val repo = FakePresetsRepository(Presets(0, listOf(Preset("a", provider, "e", "m", "k"))))
        val flow = PresetsStateFlow(repo)
        val interactor = PresetsListStateInteractor(flow)
        interactor.load()
        interactor.updatePreset(0, Preset("a", provider, "e", "m2", "k"))
        assertEquals("m2", repo.data.presets[0].model)
    }

    @Test
    fun deletePresetRemovesGiven() = runBlocking {
        val provider = LlmProvider("p", "e", emptyList())
        val repo = FakePresetsRepository(
            Presets(0, listOf(Preset("a", provider, "e", "m", "k"), Preset("b", provider, "e", "m", "k")))
        )
        val flow = PresetsStateFlow(repo)
        val interactor = PresetsListStateInteractor(flow)
        interactor.load()
        interactor.deletePreset(0)
        assertEquals(1, repo.data.presets.size)
        assertEquals(0, repo.data.active)
    }
}

