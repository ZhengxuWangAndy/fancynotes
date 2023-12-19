package com.example.fancynotes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen {
    MAIN_SCREEN, EDIT_SCREEN, CANVAS_SCREEN
}

enum class SortOption {
    DATE, PRIORITY
}

object CurrentMain {
    var SCREEN_STATE by mutableStateOf(Screen.MAIN_SCREEN)
    var EDIT_NOTE_INDEX by mutableStateOf(EditMode.INSERT)
    var NOTES = mutableListOf<Note>()
    var SORT_OPTION by mutableStateOf(SortOption.DATE)
}

val defaultNotes = arrayOf(
    Note("Tap notes to edit", "Try now", 1, "Mon, Dec 4, 2023"),
    Note("Tap plus to add new one", "Start to create your notes", 2, "Mon, Dec 4, 2023"),
    Note("Example", "What to eat?","Mon, Dec 4, 2023"),
)
