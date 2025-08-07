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
}

