package com.facevideo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facevideo.app.data.DemoData
import com.facevideo.app.model.ChatMessage
import com.facevideo.app.model.ChatSummary
import java.util.UUID

private val Night = Color(0xFF0A1020)
private val Panel = Color(0xFF141E32)
private val PanelLight = Color(0xFF1B2942)
private val Mint = Color(0xFF65E6D2)
private val MainText = Color(0xFFF4F6FC)
private val Muted = Color(0xFF91A0BA)
private val AvatarColors = listOf(
    Color(0xFF287F83), Color(0xFF6756A8), Color(0xFF986B47), Color(0xFF3E7298)
)

private enum class MainTab(val title: String, val symbol: String) {
    CHATS("Чаты", "✉"),
    CALLS("Звонки", "◉"),
    CONTACTS("Контакты", "◎"),
    PROFILE("Профиль", "◌")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Mint,
                    background = Night,
                    surface = Panel,
                    onPrimary = Night,
                    onBackground = MainText,
                    onSurface = MainText
                )
            ) {
                FirebaseAuthGate { userEmail, onSignOut ->
                    FaceVideoApp(userEmail = userEmail, onSignOut = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun FaceVideoApp(userEmail: String?, onSignOut: () -> Unit) {
    var selectedTab by remember { mutableStateOf(MainTab.CHATS) }
    var openedChat by remember { mutableStateOf<ChatSummary?>(null) }
    var showCallInfo by remember { mutableStateOf(false) }
    val messagesByChat = remember {
        mutableStateMapOf<String, SnapshotStateList<ChatMessage>>().apply {
            DemoData.chats.forEach { chat ->
                put(chat.id, mutableStateListOf<ChatMessage>().apply {
                    addAll(DemoData.messages[chat.id].orEmpty())
                })
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Night, Color(0xFF101A30), Night))
        )
    ) {
        if (openedChat != null) {
            val chat = openedChat!!
            ChatScreen(
                chat = chat,
                messages = messagesByChat[chat.id] ?: mutableStateListOf(),
                onBack = { openedChat = null },
                onCall = { showCallInfo = true }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                when (selectedTab) {
                    MainTab.CHATS -> ChatListScreen(onOpenChat = { openedChat = it })
                    MainTab.CALLS -> CallsScreen(onCall = { showCallInfo = true })
                    MainTab.CONTACTS -> ContactsScreen(onOpenChat = { openedChat = it })
                    MainTab.PROFILE -> ProfileScreen(userEmail = userEmail, onSignOut = onSignOut)
                }
                BottomNavigation(selectedTab = selectedTab, onSelect = { selectedTab = it })
            }
        }
    }

    if (showCallInfo) {
        AlertDialog(
            onDismissRequest = { showCallInfo = false },
            containerColor = PanelLight,
            title = { Text("Видеозвонки", color = MainText, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Экран и кнопки звонка подготовлены как следующий модуль. Для реальных звонков нужно подключить WebRTC и сигнальный сервер; сейчас соединение ещё не устанавливается.",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { showCallInfo = false }) {
                    Text("Понятно", color = Mint)
                }
            }
        )
    }
}

@Composable
private fun ChatListScreen(onOpenChat: (ChatSummary) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        com.facevideo.app.model.filterChats(DemoData.chats, query)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("FACE VIDEO", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text("Сообщения", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = Color(0xFF173B3B),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFF28605D))
            ) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Mint))
                    Spacer(Modifier.width(7.dp))
                    Text("DEMO", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        SearchField(value = query, onValueChange = { query = it })

        Text(
            "БЫСТРЫЙ ДОСТУП",
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 12.dp)
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            DemoData.chats.filter { it.online }.forEach { chat ->
                Column(
                    Modifier.width(62.dp).clickable { onOpenChat(chat) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(chat.initials, chat.avatarStyle, Modifier.size(54.dp), highlighted = true)
                    Spacer(Modifier.height(7.dp))
                    Text(chat.title.substringBefore(" "), color = MainText, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Недавние", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(filtered.size.toString() + " диалогов", color = Muted, fontSize = 11.sp)
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filtered, key = { it.id }) { chat ->
                ChatRow(chat = chat, onClick = { onOpenChat(chat) })
            }
            if (filtered.isEmpty()) {
                item {
                    Text("Ничего не найдено", color = Muted, modifier = Modifier.fillMaxWidth().padding(28.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = Panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF24324B))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = MainText, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", color = Mint, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text("Поиск по чатам", color = Muted, fontSize = 14.sp)
                        inner()
                    }
                }
            }
        )
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .background(Color(0x66151F33)).padding(horizontal = 11.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.initials, chat.avatarStyle, Modifier.size(53.dp), highlighted = chat.online)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (chat.online) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Mint))
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(chat.preview, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.time, color = Muted, fontSize = 10.sp)
            if (chat.unreadCount > 0) {
                Spacer(Modifier.height(7.dp))
                Box(Modifier.size(21.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                    Text(chat.unreadCount.toString(), color = Night, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Avatar(
    initials: String,
    style: Int,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Box(
        modifier.clip(CircleShape).background(
            Brush.linearGradient(listOf(AvatarColors[style % AvatarColors.size], PanelLight))
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (highlighted) {
            Box(Modifier.align(Alignment.BottomEnd).padding(1.dp).size(10.dp).clip(CircleShape).background(Mint))
        }
    }
}

@Composable
private fun ChatScreen(
    chat: ChatSummary,
    messages: SnapshotStateList<ChatMessage>,
    onBack: () -> Unit,
    onCall: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = Mint, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp))
            Avatar(chat.initials, chat.avatarStyle, Modifier.size(43.dp), highlighted = chat.online)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = MainText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (chat.online) "в сети" else "был(а) недавно", color = if (chat.online) Mint else Muted, fontSize = 11.sp)
            }
            Text("☎", color = Mint, fontSize = 21.sp, modifier = Modifier.clickable(onClick = onCall).padding(7.dp))
            Text("▣", color = Mint, fontSize = 21.sp, modifier = Modifier.clickable(onClick = onCall).padding(7.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF25324A)))

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                Text(
                    "СЕГОДНЯ",
                    color = Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            items(messages, key = { it.id }) { message -> MessageBubble(message) }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = PanelLight, shape = CircleShape, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("+", color = Mint, fontSize = 25.sp) }
            }
            Spacer(Modifier.width(9.dp))
            Surface(
                Modifier.weight(1f),
                color = Panel,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFF2B3B56))
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (draft.isNotBlank()) {
                            messages.add(ChatMessage(UUID.randomUUID().toString(), draft.trim(), "сейчас", true))
                            draft = ""
                        }
                    }),
                    textStyle = TextStyle(color = MainText, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("Сообщение...", color = Muted, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.width(9.dp))
            Box(
                Modifier.size(43.dp).clip(CircleShape).background(Mint).clickable {
                    if (draft.isNotBlank()) {
                        messages.add(ChatMessage(UUID.randomUUID().toString(), draft.trim(), "сейчас", true))
                        draft = ""
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = Night, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Локальный прототип · сообщения не отправляются в интернет",
            color = Muted,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 7.dp)
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isMine) Color(0xFF245E5D) else PanelLight,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (message.isMine) 18.dp else 5.dp,
                bottomEnd = if (message.isMine) 5.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(message.text, color = MainText, fontSize = 14.sp)
                Text(
                    message.time,
                    color = Color(0xFFB8C9D0),
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun CallsScreen(onCall: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp)) {
        Text("FACE VIDEO", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Text("Звонки", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFF273750))) {
            Column(Modifier.fillMaxWidth().padding(19.dp)) {
                Text("Связь без границ", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Личные и групповые аудио- и видеозвонки появятся после подключения WebRTC.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(17.dp))
                Surface(
                    color = Color(0xFF214D4B),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.clickable(onClick = onCall)
                ) {
                    Text("Проверить видеозвонки  →", color = Mint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp))
                }
            }
        }
        Spacer(Modifier.height(25.dp))
        Text("НЕДАВНИЕ", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        DemoData.chats.take(3).forEach { chat ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(chat.initials, chat.avatarStyle, Modifier.size(47.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(chat.title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("История звонков появится после подключения сервера", color = Muted, fontSize = 10.sp)
                }
                Text("↗", color = Mint, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun ContactsScreen(onOpenChat: (ChatSummary) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp)) {
        Text("FACE VIDEO", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Text("Контакты", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Ваши контакты появятся после регистрации и синхронизации.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        DemoData.chats.forEach { chat ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpenChat(chat) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(chat.initials, chat.avatarStyle, Modifier.size(48.dp), chat.online)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(chat.title, color = MainText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(if (chat.online) "В сети" else "Не в сети", color = Muted, fontSize = 11.sp)
                }
                Text("Сообщение  ›", color = Mint, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProfileScreen(userEmail: String?, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FACE VIDEO", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(40.dp))
        Avatar("FV", 1, Modifier.size(92.dp), highlighted = true)
        Spacer(Modifier.height(15.dp))
        Text("Ваш профиль", color = MainText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(userEmail ?: "Демонстрационный режим", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(28.dp))
        listOf("Личные данные", "Уведомления", "Конфиденциальность", "О приложении").forEachIndexed { index, label ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(listOf("◉", "♧", "▣", "ⓘ")[index], color = Mint, fontSize = 17.sp)
                Spacer(Modifier.width(13.dp))
                Text(label, color = MainText, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("›", color = Muted, fontSize = 20.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (userEmail != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color(0xFF3B2631),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSignOut)
            ) {
                Text(
                    "Выйти из аккаунта",
                    color = Color(0xFFFFA5B5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavigation(selectedTab: MainTab, onSelect: (MainTab) -> Unit) {
    Surface(color = Color(0xFF10192B), shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.values().forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(tab.symbol, color = if (selected) Mint else Muted, fontSize = 20.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.title,
                        color = if (selected) Mint else Muted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
