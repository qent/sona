package io.qent.sona.core.state

import io.qent.sona.core.presets.LlmProvider
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakePresetsRepository(var data: Presets = Presets(0, emptyList())) : PresetsRepository {
    override suspend fun load(): Presets = data
    override suspend fun save(presets: Presets) { data = presets }
}

class PresetsStateInteractorTest {
    @Test
    fun addPresetUpdatesRepository() = runBlocking {
        val provider = LlmProvider("p", "e", emptyList())
        val repo = FakePresetsRepository(Presets(0, emptyList()))
        val interactor = PresetsStateInteractor(repo)
        interactor.load()
        interactor.startCreatePreset()
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
        val interactor = PresetsStateInteractor(repo)
        interactor.load()
        interactor.selectPreset(1)
        assertEquals(1, repo.data.active)
    }

    @Test
    fun savePresetUpdatesRepository() = runBlocking {
        val provider = LlmProvider("p", "e", emptyList())
        val repo = FakePresetsRepository(Presets(0, listOf(Preset("a", provider, "e", "m", "k"))))
        val interactor = PresetsStateInteractor(repo)
        interactor.load()
        interactor.savePreset(Preset("a", provider, "e", "m2", "k"))
        assertEquals("m2", repo.data.presets[0].model)
    }

    @Test
    fun deletePresetRemovesCurrent() = runBlocking {
        val provider = LlmProvider("p", "e", emptyList())
        val repo = FakePresetsRepository(
            Presets(0, listOf(Preset("a", provider, "e", "m", "k"), Preset("b", provider, "e", "m", "k")))
        )
        val interactor = PresetsStateInteractor(repo)
        interactor.load()
        interactor.deletePreset()
        assertEquals(1, repo.data.presets.size)
        assertEquals(0, repo.data.active)
    }

    @Test
    fun startAndFinishCreatePresetToggleFlag() = runBlocking {
        val repo = FakePresetsRepository(Presets(0, emptyList()))
        val interactor = PresetsStateInteractor(repo)
        interactor.startCreatePreset()
        assertEquals(true, interactor.creatingPreset)
        interactor.finishCreatePreset()
        assertEquals(false, interactor.creatingPreset)
    }
}

