package com.eungpang.applocker.presentation.main

import android.app.Application
import androidx.lifecycle.*
import com.eungpang.applocker.domain.item.Item

class MainViewModel(app: Application) : AndroidViewModel(app), ItemHandler {

    private val _actionState = MutableLiveData<ActionState>()
    val actionState: LiveData<ActionState> = _actionState

    sealed class ActionState {
        class LaunchSns(val item: Item): ActionState()
    }

    override fun onClick(item: Item) {
        _actionState.value = ActionState.LaunchSns(item)
    }

    override fun onItemLongClick(item: Item) {
        // do nothing now
    }

    class ViewModelFactory(private val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(app) as T
        }
    }
}

interface ItemHandler {
    fun onClick(item: Item)
    fun onItemLongClick(item: Item)
}