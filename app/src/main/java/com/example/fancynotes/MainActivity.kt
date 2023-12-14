package com.example.fancynotes

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.fancynotes.ui.theme.White
import com.example.fancynotes.ui.theme.bottom_dp
import com.example.fancynotes.ui.theme.fancyTheme
import com.example.fancynotes.ui.theme.icons_dp
import com.example.fancynotes.ui.theme.medium_dp
import com.example.fancynotes.ui.theme.min_dp
import com.example.fancynotes.ui.theme.radius_dp
import com.example.fancynotes.ui.theme.smaller_dp
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import android.graphics.Canvas
import android.graphics.Color as Color2
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import androidx.activity.compose.BackHandler


//import com.example.fancynotes.ui.theme.*

enum class Screen {
    MAIN_SCREEN, EDIT_SCREEN, CANVAS_SCREEN
}

val defaultNotes = arrayOf(
    Note("Tap notes to edit", "Try now", 1, "Mon, Dec 4, 2023"),
    Note("Tap plus to add new one", "Start to create your notes", 2, "Mon, Dec 4, 2023"),
    Note("Example", "What to eat?","Mon, Dec 4, 2023"),
)

enum class SortOption {
    DATE, PRIORITY
}
object Current {
    var SCREEN_STATE by mutableStateOf(Screen.MAIN_SCREEN)
    var EDIT_NOTE_INDEX by mutableStateOf(EditMode.INSERT)
    var NOTES = mutableListOf<Note>()
    var SORT_OPTION by mutableStateOf(SortOption.DATE)
}



class MainActivity : ComponentActivity() {
    private val dbHelper = NoteDbHelper(this)
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInResultLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_APPLICATION_CLIENT_ID")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        Current.NOTES = queryAllNote(dbHelper)
        if (Current.NOTES.isEmpty()) {

            for(note in defaultNotes) {
                insertNote(dbHelper, note)
            }
        }

        // Check Login
        signInResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
                Toast.makeText(this, R.string.loginMore, Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(this, R.string.login, Toast.LENGTH_SHORT).show()
        }

        setContent {
            fancyTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {

                    Current.NOTES = queryAllNote(dbHelper)

                    when (Current.SCREEN_STATE) {
                        Screen.MAIN_SCREEN -> MainScreenDisplay(this@MainActivity::performGoogleSignIn)
                        Screen.EDIT_SCREEN -> EditScreenDisplay(dbHelper, this@MainActivity::performGoogleSignIn)
                        Screen.CANVAS_SCREEN -> CanvasScreenDisplay(dbHelper = dbHelper)
                    }
                }
            }
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        Toast.makeText(this, R.string.login, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    fun performGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInResultLauncher.launch(signInIntent)
    }
}

@Composable
fun MainScreenTopBar(onGoogleSignIn: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(medium_dp)) {
            Text(text = stringResource(id = R.string.whimsiNote), fontSize = 24.sp)


            Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                // Google Sign-In Button
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = stringResource(id = R.string.googleBtn)
                    )
                }

                // Menu Button
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        painter = painterResource(id = R.drawable.more),
                        tint = Color.White,
                        contentDescription = stringResource(id = R.string.moreBtn)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        Current.SORT_OPTION = SortOption.DATE
                    }) {
                        Text(stringResource(id = R.string.sortDate))
                    }

                    DropdownMenuItem(onClick = {
                        showMenu = false
                        Current.SORT_OPTION = SortOption.PRIORITY
                    }) {
                        Text(stringResource(id = R.string.sortPriority))
                    }

//                    DropdownMenuItem(onClick = {
//                        showMenu = false
//                    }) {
//                        Text(stringResource(id = R.string.selectDate))
//                    }
                }
            }
        }

    }
}

@Composable
fun MainScreenBottomBar() {
    BottomAppBar(backgroundColor = White){
        Row (modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End ) {
            Icon(
                painter = painterResource(id = R.drawable.add2),
                tint = Color.Black,
                contentDescription = stringResource(id = R.string.addNote),
                modifier = Modifier
                    .size(radius_dp, radius_dp)
                    .padding(min_dp)
                    .clickable {
                        Current.EDIT_NOTE_INDEX = EditMode.INSERT
                        Current.SCREEN_STATE = Screen.EDIT_SCREEN
                    }
            )

        }
    }
}

@Composable
fun SearchBar(searchText: String, onSearchTextChanged: (String) -> Unit) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(id = R.string.searchIcon)
            )
        },
        placeholder = {
            Text(
                text = stringResource(id = R.string.search),
                color = Color.Gray
            )
        }
    )
}



@Composable
fun NoteListDisplay(notes: MutableList<Note>, searchText: String) {
    val filteredNote = if (searchText.isEmpty()) {
        notes
    } else {
        notes.filter {
                note -> note.title.contains(searchText, ignoreCase = true)
                || note.content.contains(searchText, ignoreCase = true)
        }
    }
    val sortedNotes = when (Current.SORT_OPTION) {
        SortOption.DATE -> filteredNote.sortedBy { it.date }
        SortOption.PRIORITY -> filteredNote.sortedByDescending { it.importance }
    }
    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(smaller_dp)) {
        for (note in sortedNotes) {
            NoteCard(msg = note, index = notes.indexOf(note))
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
fun MainScreenDisplay(onGoogleSignIn: () -> Unit) {
    var searchText by remember { mutableStateOf("") }

    BackHandler(onBack = {
        Current.SCREEN_STATE = Screen.MAIN_SCREEN
    })

    Column {
        MainScreenTopBar(onGoogleSignIn)
        Spacer(modifier = Modifier.height(smaller_dp))
        SearchBar(searchText = searchText, onSearchTextChanged = { searchText = it })
        Spacer(modifier = Modifier.height(smaller_dp))
        NoteListDisplay(
            Current.NOTES, searchText = searchText
        )
        Spacer(modifier = Modifier.height(bottom_dp))
        MainScreenBottomBar()
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
//        Row {
//            Button(onClick = {
//                Current.SCREEN_STATE = Screen.CANVAS_SCREEN
//            }) {
//                Text(text = stringResource(id = R.string.canvas))
//            }
//        }
        CanvasScreenDisplay(dbHelper)
//        Box(modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.BottomCenter) {
//            Row {
//                Button(onClick = {
//                    insertNote(dbHelper, note)
//                    Current.SCREEN_STATE = Screen.MAIN_SCREEN
//                }) {
//                    Text(stringResource(id = R.string.save))
//                }
//                Spacer(modifier = Modifier.width(medium_dp))
//                Button(onClick = {
//                    Current.SCREEN_STATE = Screen.MAIN_SCREEN
//                }) {
//                    Text(stringResource(id = R.string.back))
//                }
//            }
//        }
    }
}



@Composable
fun EditScreenDisplay(dbHelper: NoteDbHelper, onGoogleSignIn: () -> Unit) {
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

@Composable
fun CanvasScreenDisplay(dbHelper: NoteDbHelper) {
    val note = when (Current.EDIT_NOTE_INDEX) {
        EditMode.INSERT -> Note("", "")
        else -> Current.NOTES[Current.EDIT_NOTE_INDEX]
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        DrawingCanvas(dbHelper, note)
    }
}

@Composable
fun DrawingCanvas(dbHelper: NoteDbHelper, note: Note) {
    val lines = remember {
        mutableStateListOf<Line>()
    }
    Column {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .background(Color.DarkGray)
                .fillMaxWidth()
                .height(100.dp)
                .pointerInput(true) {
                    detectDragGestures { change, dragAmount ->
                        change.consumeAllChanges()

                        val line = Line(
                            start = change.position - dragAmount,
                            end = change.position
                        )

                        lines.add(line)
                    }
                }
        ) {
            lines.forEach { line ->
                drawLine(
                    color = line.color,
                    start = line.start,
                    end = line.end,
                    strokeWidth = line.strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter) {
            Row {
                Button(onClick = {
                    // Save the drawing as an image
                    val imageBytes = saveCanvasAsImage(lines)
                    // Update the 'image' property of the note
                    note.image = imageBytes
                    insertNote(dbHelper, note)
                    Current.SCREEN_STATE = Screen.MAIN_SCREEN
                }) {
                    Text(stringResource(id = R.string.save))
                }
                Spacer(modifier = Modifier.width(medium_dp))
                Button(onClick = {
                    Current.SCREEN_STATE = Screen.MAIN_SCREEN
                }) {
                    Text(stringResource(id = R.string.back))
                }
            }
        }
    }

}

fun saveCanvasAsImage(lines: List<Line>): ByteArray? {
    val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color2.WHITE)
    val paint = Paint().apply {
        isAntiAlias = true
        color = Color2.BLACK
        strokeWidth = 5f
    }
    lines.forEach { line ->
        canvas.drawLine(
            line.start.x,
            line.start.y,
            line.end.x,
            line.end.y,
            paint
        )
    }
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}
data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 1.dp
)


