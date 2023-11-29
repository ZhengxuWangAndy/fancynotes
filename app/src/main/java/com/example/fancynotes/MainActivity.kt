package com.example.fancynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.fancynotes.ui.theme.ImportanceColors
import com.example.fancynotes.ui.theme.fancyTheme
import com.example.fancynotes.ui.theme.icons_dp
import com.example.fancynotes.ui.theme.medium_dp
import com.example.fancynotes.ui.theme.min_dp
import com.example.fancynotes.ui.theme.radius_dp
import com.example.fancynotes.ui.theme.smaller_dp
//import com.example.fancynotes.ui.theme.*

enum class Screen {
    MAIN_SCREEN, EDIT_SCREEN
}

val defaultNotes = arrayOf(
    Note("Tap notes to edit", "Try now", 1),
    Note("Tap plus to add new one", "Start to create your notes", 2),
    Note("Example", "What to eat?"),
)

object Current {
    var SCREEN_STATE by mutableStateOf(Screen.MAIN_SCREEN)
    var EDIT_NOTE_INDEX by mutableStateOf(EditMode.INSERT)
    var NOTES = mutableListOf<Note>()
}

class MainActivity : ComponentActivity() {
    private val dbHelper = NoteDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Current.NOTES = queryAllNote(dbHelper)
        if (Current.NOTES.isEmpty()) {

            for(note in defaultNotes) {
                insertNote(dbHelper, note)
            }
        }

        setContent {
            fancyTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {

                    Current.NOTES = queryAllNote(dbHelper)

                    when (Current.SCREEN_STATE) {
                        Screen.MAIN_SCREEN -> MainScreenDisplay()
                        Screen.EDIT_SCREEN -> EditScreenDisplay(dbHelper)
                    }
                }
            }
        }
    }

    override fun onDestroy() {

        dbHelper.close()
        super.onDestroy()
    }
}

@Composable
fun MainScreenTopBar() {
    TopAppBar {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(medium_dp)) {
            Text(text = "Fancy Note", fontSize = 24.sp)

            Row (modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End ) {
                Icon(
                    painter = painterResource(id = R.drawable.add2),
                    tint = Color.White,
                    contentDescription = "Add Note Button",
                    modifier = Modifier
                        .size(icons_dp, icons_dp)
                        .clickable {
                            Current.EDIT_NOTE_INDEX = EditMode.INSERT
                            Current.SCREEN_STATE = Screen.EDIT_SCREEN
                        }
                )
            }
        }
    }
}

@Composable
fun NoteListDisplay(notes: MutableList<Note>) {
    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(smaller_dp)) {
        for (i in notes.indices) {
            NoteCard(msg = notes[i], index = i)
        }
    }
}

@Composable
fun NoteCard(msg: Note, index: Int) {
    Row(modifier = Modifier
        .padding(vertical = min_dp / 2)
        .fillMaxWidth()
        .clickable {
            Current.EDIT_NOTE_INDEX = index
            Current.SCREEN_STATE = Screen.EDIT_SCREEN
        }
        .padding(all = min_dp)
    ) {
        Box(modifier = Modifier
            .clip(CircleShape)
            .background(color = ImportanceColors[msg.importance])
            .size(width = radius_dp, height = radius_dp))

        Spacer(modifier = Modifier.width(smaller_dp))

        Column {
            Text(text = msg.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(min_dp /2))
            Text(text = msg.content, color = Color(0x99000000))
        }
    }
}

@Composable
fun MainScreenDisplay() {
    Column {
        MainScreenTopBar()
        Spacer(modifier = Modifier.height(smaller_dp))
        NoteListDisplay(
            Current.NOTES
        )
    }
}

@Composable
fun EditNoteTopBar(dbHelper: NoteDbHelper) {
    TopAppBar {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(medium_dp)) {
            Text(text = "Fancy Note", fontSize = 24.sp)
            Row (modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    tint = Color.White,
                    contentDescription = "Delete Note Button",
                    modifier = Modifier
                        .size(icons_dp, icons_dp)
                        .clickable {
                            if (Current.EDIT_NOTE_INDEX != EditMode.INSERT) {
                                deleteNote(
                                    dbHelper,
                                    getID(dbHelper, Current.NOTES[Current.EDIT_NOTE_INDEX].title)
                                )
                            }
                            Current.SCREEN_STATE = Screen.MAIN_SCREEN
                        }
                )
            }
        }
    }
}

@Composable
fun EditNote(dbHelper: NoteDbHelper, note: Note) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var importanceSlider by remember { mutableStateOf(note.importance.toFloat()) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(medium_dp)) {
        Text(text = "Title")
        OutlinedTextField(
            value = title,
            onValueChange = {title = it; note.title = title; },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text(text = "Content", modifier = Modifier.padding(top = medium_dp))
        OutlinedTextField(
            value = content,
            onValueChange = {content = it; note.content = content},
            modifier = Modifier.fillMaxWidth(),
            maxLines = 8,
            singleLine = false
        )
        Text(text = "Priority(1-5)", modifier = Modifier.padding(top = medium_dp))
        Slider(
            value = importanceSlider,
            onValueChange = { importanceSlider = it },
            valueRange = 0f..4f,
            onValueChangeFinished = {
                note.importance = importanceSlider.toInt()
            },
            steps = 3,
        )
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter) {
            Row {
                Button(onClick = {
                    insertNote(dbHelper, note)
                    Current.SCREEN_STATE = Screen.MAIN_SCREEN
                }) {
                    Text(text = "save")
                }
                Spacer(modifier = Modifier.width(medium_dp))
                Button(onClick = {
                    Current.SCREEN_STATE = Screen.MAIN_SCREEN
                }) {
                    Text(text = "back")
                }
            }
        }
    }
}

@Composable
fun EditScreenDisplay(dbHelper: NoteDbHelper) {
    val note = when (Current.EDIT_NOTE_INDEX) {
        EditMode.INSERT -> Note("", "")
        else -> Current.NOTES[Current.EDIT_NOTE_INDEX]
    }
    Column {
        EditNoteTopBar(dbHelper)
        Spacer(modifier = Modifier.height(smaller_dp))
        EditNote(dbHelper, note)
    }
}