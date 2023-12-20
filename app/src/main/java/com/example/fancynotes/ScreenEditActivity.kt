package com.example.fancynotes

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fancynotes.ui.theme.fancyTheme
import com.example.fancynotes.ui.theme.icons_dp
import com.example.fancynotes.ui.theme.medium_dp
import com.example.fancynotes.ui.theme.smaller_dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class EditScreenActivity : ComponentActivity() {
    private val dbHelper = NoteDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            fancyTheme {
                // A surface container using the 'background' color from the theme
                androidx.compose.material.Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material.MaterialTheme.colors.background) {

                    CurrentMain.NOTES = queryAllNote(dbHelper, CurrentMain.SORT_OPTION)

                    when (CurrentMain.SCREEN_STATE) {
                        Screen.EDIT_SCREEN -> EditScreenDisplay(dbHelper, this@EditScreenActivity)
                        else -> {}
                    }
                }
            }
        }
    }
}


@Composable
fun EditNoteTopBar(dbHelper: NoteDbHelper) {
    TopAppBar {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(medium_dp)) {
            Text(text = stringResource(id = R.string.whimsiNote), fontSize = 24.sp)
            Row (modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    tint = Color.White,
                    contentDescription = stringResource(id = R.string.deleteNote),
                    modifier = Modifier
                        .size(icons_dp, icons_dp)
                        .clickable {
                            if (CurrentMain.EDIT_NOTE_INDEX != EditMode.INSERT) {
                                deleteNote(
                                    dbHelper,
                                    getID(dbHelper, CurrentMain.NOTES[CurrentMain.EDIT_NOTE_INDEX].title)
                                )
                            }
                            CurrentMain.SCREEN_STATE = Screen.MAIN_SCREEN
                        }
                )
            }
        }
    }
}


@Composable
fun EditNote(dbHelper: NoteDbHelper, note: Note) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var importanceSlider by remember { mutableStateOf(note.importance.toFloat()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateText by remember { mutableStateOf(note.date) }

    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                    selectedDateText = sdf.format(calendar.time) // Update the text to show the selected date
                    note.date = selectedDateText
                    showDatePicker = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(medium_dp)) {
        Row {
            Button(onClick = { showDatePicker = true }) {
                Text(text = stringResource(id = R.string.selectDate))
            }
            if (selectedDateText.isNotEmpty()) {
                Text(text = selectedDateText, modifier = Modifier.padding(start = medium_dp))
            }
        }
        Text(text = stringResource(id = R.string.title))
        OutlinedTextField(
            value = title,
            onValueChange = {title = it; note.title = title; },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text(text = stringResource(id = R.string.content), modifier = Modifier.padding(top = medium_dp))
        OutlinedTextField(
            value = content,
            onValueChange = {content = it; note.content = content},
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxLines = 8,
            singleLine = false
        )
        Text(text = stringResource(id = R.string.priority), modifier = Modifier.padding(top = medium_dp))
        Slider(
            value = importanceSlider,
            onValueChange = { importanceSlider = it },
            valueRange = 0f..4f,
            onValueChangeFinished = {
                note.importance = importanceSlider.toInt()
            },
            steps = 3,
        )
        Spacer(modifier = Modifier.height(smaller_dp))
        Row {
            Button(onClick = {
                val intent = Intent(context, ScreenCanvasHCRActivity::class.java)
                context.startActivity(intent)
            }) {
                Text(text = stringResource(id = R.string.canvas))
            }
        }

        Box(modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter) {
            Row {
                Button(onClick = {
                    insertNote(dbHelper, note)
                    CurrentMain.SCREEN_STATE = Screen.MAIN_SCREEN
                }) {
                    Text(stringResource(id = R.string.save))
                }
                Spacer(modifier = Modifier.width(medium_dp))
                Button(onClick = {
                    CurrentMain.SCREEN_STATE = Screen.MAIN_SCREEN
                }) {
                    Text(stringResource(id = R.string.back))
                }
            }
        }
    }
}

@Composable
fun EditScreenDisplay(dbHelper: NoteDbHelper, editScreenActivity: EditScreenActivity) {
    val note = when (CurrentMain.EDIT_NOTE_INDEX) {
        EditMode.INSERT -> Note("", "")
        else -> CurrentMain.NOTES[CurrentMain.EDIT_NOTE_INDEX]
    }
    Column {
        EditNoteTopBar(dbHelper)
        Spacer(modifier = Modifier.height(smaller_dp))
        EditNote(dbHelper, note)
    }
}
