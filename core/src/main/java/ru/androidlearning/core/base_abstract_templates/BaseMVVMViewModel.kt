package ru.androidlearning.core.base_abstract_templates

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import ru.androidlearning.core.DataLoadingState
import ru.androidlearning.core.DictionaryPresentationDataModel

abstract class BaseMVVMViewModel : ViewModel() {

    protected val dataLoadingLiveData = MutableLiveData<DataLoadingState<DictionaryPresentationDataModel>>()
    fun getLiveData(): LiveData<DataLoadingState<DictionaryPresentationDataModel>> = dataLoadingLiveData

    protected val coroutineScopeIO by lazy {
        CoroutineScope(
            Dispatchers.IO
                    + SupervisorJob()
                    + CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }
        )
    }

    override fun onCleared() {
        coroutineScopeIO.cancel()
        super.onCleared()
    }
}
