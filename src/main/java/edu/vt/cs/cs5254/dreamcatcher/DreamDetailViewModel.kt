package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.lang.IllegalArgumentException
import java.util.UUID

class DreamDetailViewModel : ViewModel() {

    private val dreamRepository = DreamRepository.get()
    private val dreamIdLiveData = MutableLiveData<UUID>()

    var DreamWithEntriesLiveData: LiveData<DreamWithEntries> =
            Transformations.switchMap(dreamIdLiveData) { dreamId ->
                dreamRepository.getDreamWithEntries(dreamId)
            }

    fun loadDream(dreamId: UUID) {
        dreamIdLiveData.value = dreamId
    }

    fun saveDream(dreamWithEntries: DreamWithEntries) {
        dreamRepository.updateDreamWithEntries(dreamWithEntries)
    }

    fun addDreamEntry(dreamEntry: DreamEntry) {
        dreamRepository.addDreamEntry(dreamEntry)
    }

    fun deleteDreamEntry(dreamId: UUID, kind: DreamEntryKind) {
        dreamRepository.deleteDreamEntry(dreamId, kind)
    }

    fun deleteDreamEntry(id: UUID) {
        dreamRepository.deleteDreamEntry(id)
    }

    fun getPhotoFile(dreamWithEntries: DreamWithEntries): File {
        return dreamRepository.getPhotoFile(dreamWithEntries.dream)
    }

}