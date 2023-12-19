package com.example.fancynotes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fancynotes.ui.theme.ImportanceColors
import com.example.fancynotes.ui.theme.White
import com.example.fancynotes.ui.theme.bottom_dp
import com.example.fancynotes.ui.theme.fancyTheme
import com.example.fancynotes.ui.theme.medium_dp
import com.example.fancynotes.ui.theme.min_dp
import com.example.fancynotes.ui.theme.radius_dp
import com.example.fancynotes.ui.theme.smaller_dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task


class MainScreenActivity : ComponentActivity() {
    private val dbHelper = NoteDbHelper(this)
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("HELLO")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        CurrentMain.NOTES = queryAllNote(dbHelper)
        if (CurrentMain.NOTES.isEmpty()) {

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
                androidx.compose.material.Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material.MaterialTheme.colors.background) {

                    CurrentMain.NOTES = queryAllNote(dbHelper)

                    when (CurrentMain.SCREEN_STATE) {
                        Screen.MAIN_SCREEN -> MainScreenDisplay(this@MainScreenActivity::performGoogleSignIn)
                        Screen.EDIT_SCREEN -> EditScreenDisplay(dbHelper, EditScreenActivity())
                        else -> {}
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
            androidx.compose.material.Text(text = stringResource(id = R.string.whimsiNote), fontSize = 24.sp)


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
                        CurrentMain.SORT_OPTION = SortOption.DATE
                    }) {
                        androidx.compose.material.Text(stringResource(id = R.string.sortDate))
                    }

                    DropdownMenuItem(onClick = {
                        showMenu = false
                        CurrentMain.SORT_OPTION = SortOption.PRIORITY
                    }) {
                        androidx.compose.material.Text(stringResource(id = R.string.sortPriority))
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
                        CurrentMain.EDIT_NOTE_INDEX = EditMode.INSERT
                        CurrentMain.SCREEN_STATE = Screen.EDIT_SCREEN
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
            androidx.compose.material.Text(
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
    val sortedNotes = when (CurrentMain.SORT_OPTION) {
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
            CurrentMain.EDIT_NOTE_INDEX = index
            CurrentMain.SCREEN_STATE = Screen.EDIT_SCREEN
        }
        .padding(all = min_dp)
    ) {
        Box(modifier = Modifier
            .clip(CircleShape)
            .background(color = ImportanceColors[msg.importance])
            .size(width = radius_dp, height = radius_dp))

        Spacer(modifier = Modifier.width(smaller_dp))

        Column {
            androidx.compose.material.Text(text = msg.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(min_dp /2))
            androidx.compose.material.Text(text = msg.content, color = Color(0x99000000))
        }
    }
}


@Composable
fun MainScreenDisplay(onGoogleSignIn: () -> Unit) {
    var searchText by remember { mutableStateOf("") }

    BackHandler(onBack = {
        CurrentMain.SCREEN_STATE = Screen.MAIN_SCREEN
    })

    Column {
        MainScreenTopBar(onGoogleSignIn)
        Spacer(modifier = Modifier.height(smaller_dp))
        SearchBar(searchText = searchText, onSearchTextChanged = { searchText = it })
        Spacer(modifier = Modifier.height(smaller_dp))
        NoteListDisplay(
            CurrentMain.NOTES, searchText = searchText
        )
        Spacer(modifier = Modifier.height(bottom_dp))
        MainScreenBottomBar()
    }
}



