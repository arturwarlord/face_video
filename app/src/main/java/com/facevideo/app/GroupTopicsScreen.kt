package com.facevideo.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facevideo.app.data.FaceVideoGroupTopic
import com.facevideo.app.data.GroupTopicDestination
import com.facevideo.app.data.SupabaseChatRepository
import kotlinx.coroutines.launch

private val TopicAccent = Color(0xFF5369DC)
private val TopicText = Color(0xFF202936)
private val TopicMuted = Color(0xFF7A8796)
private val TopicBorder = Color(0xFFE2E7F0)

@Composable
internal fun GroupTopicsScreen(
    repository: SupabaseChatRepository,
    userId: String,
    chatId: String,
    groupTitle: String,
    onDismiss: () -> Unit,
    onOpenTopic: (GroupTopicDestination) -> Unit
) {
    var topics by remember(chatId) { mutableStateOf<List<FaceVideoGroupTopic>>(emptyList()) }
    var isLoading by remember(chatId) { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var canManageTopics by remember(chatId, userId) { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTopic by remember { mutableStateOf<FaceVideoGroupTopic?>(null) }
    var deletingTopic by remember { mutableStateOf<FaceVideoGroupTopic?>(null) }
    var openedMenuTopicId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reloadTopics() {
        topics = repository.listGroupTopics(chatId)
    }

    LaunchedEffect(repository, chatId, userId) {
        isLoading = true
        errorMessage = null
        runCatching {
            val loadedTopics = repository.listGroupTopics(chatId)
            val membership = repository.listGroupMembers(chatId)
                .firstOrNull { it.member.userId == userId }?.member
            canManageTopics = membership?.role == "owner" ||
                (membership?.role == "admin" && membership.canChangeInfo)
            loadedTopics
        }.onSuccess {
            topics = it
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить темы группы."
        }
        isLoading = false
    }

    val topicUpdates = remember(repository, chatId) { repository.observeGroupTopics(chatId) }
    LaunchedEffect(topicUpdates) {
        topicUpdates.collect { topics = it }
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFFE2DCFF), Color(0xFFE2E9FF), Color(0xFFE8F0FF), Color(0xFFF2F5FF))
            )
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Text("‹ Назад", color = TopicAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Темы группы", color = TopicText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    groupTitle,
                    color = TopicMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canManageTopics) {
                TextButton(
                    enabled = !isBusy,
                    onClick = { showCreateDialog = true },
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text("+ Создать", color = TopicAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(Modifier.width(68.dp))
            }
        }

        Text(
            "ОБСУЖДЕНИЯ",
            modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 10.dp),
            color = TopicAccent.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        if (isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TopicAccent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item(key = "general-topic") {
                    TopicListRow(
                        icon = "💬",
                        title = "Общие",
                        subtitle = "Основная переписка группы",
                        onClick = {
                            onOpenTopic(
                                GroupTopicDestination(
                                    id = null,
                                    title = "Общие",
                                    icon = "💬",
                                    isGeneral = true
                                )
                            )
                        }
                    )
                }
                items(topics, key = { it.id }) { topic ->
                    TopicListRow(
                        icon = topic.icon,
                        title = topic.title,
                        subtitle = "Отдельная история сообщений",
                        onClick = {
                            onOpenTopic(GroupTopicDestination(topic.id, topic.title, topic.icon))
                        },
                        trailing = {
                            if (canManageTopics) {
                                Box {
                                    Text(
                                        "⋯",
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { openedMenuTopicId = topic.id }
                                            .padding(horizontal = 10.dp, vertical = 3.dp),
                                        color = TopicMuted,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    DropdownMenu(
                                        expanded = openedMenuTopicId == topic.id,
                                        onDismissRequest = { openedMenuTopicId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Изменить") },
                                            onClick = {
                                                openedMenuTopicId = null
                                                editingTopic = topic
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Удалить", color = Color(0xFFB45F6D)) },
                                            onClick = {
                                                openedMenuTopicId = null
                                                deletingTopic = topic
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                if (topics.isEmpty()) {
                    item(key = "topic-empty-state") {
                        Text(
                            "Пока нет отдельных тем. Создайте первую, чтобы разделить обсуждения по направлениям.",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
                            color = TopicMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                errorMessage.orEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                color = Color(0xFFB45F6D),
                fontSize = 11.sp
            )
        }
    }

    if (showCreateDialog) {
        GroupTopicEditDialog(
            initialTitle = "",
            initialIcon = "💬",
            isBusy = isBusy,
            title = "Новая тема",
            onDismiss = { if (!isBusy) showCreateDialog = false },
            onSave = { title, icon ->
                scope.launch {
                    isBusy = true
                    errorMessage = null
                    runCatching { repository.createGroupTopic(chatId, title, icon) }
                        .onSuccess {
                            runCatching { reloadTopics() }
                            showCreateDialog = false
                        }
                        .onFailure { errorMessage = it.message ?: "Не удалось создать тему." }
                    isBusy = false
                }
            }
        )
    }

    editingTopic?.let { topic ->
        GroupTopicEditDialog(
            initialTitle = topic.title,
            initialIcon = topic.icon,
            isBusy = isBusy,
            title = "Изменить тему",
            onDismiss = { if (!isBusy) editingTopic = null },
            onSave = { title, icon ->
                scope.launch {
                    isBusy = true
                    errorMessage = null
                    runCatching { repository.updateGroupTopic(chatId, topic.id, title, icon) }
                        .onSuccess {
                            runCatching { reloadTopics() }
                            editingTopic = null
                        }
                        .onFailure { errorMessage = it.message ?: "Не удалось изменить тему." }
                    isBusy = false
                }
            }
        )
    }

    deletingTopic?.let { topic ->
        AlertDialog(
            onDismissRequest = { if (!isBusy) deletingTopic = null },
            title = { Text("Удалить тему «${topic.title}»?") },
            text = { Text("Сообщения этой темы останутся в группе и перейдут в «Общие».") },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            errorMessage = null
                            runCatching { repository.deleteGroupTopic(chatId, topic.id) }
                                .onSuccess {
                                    runCatching { reloadTopics() }
                                    deletingTopic = null
                                }
                                .onFailure { errorMessage = it.message ?: "Не удалось удалить тему." }
                            isBusy = false
                        }
                    }
                ) {
                    Text("Удалить", color = Color(0xFFB45F6D))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTopic = null }, enabled = !isBusy) {
                    Text("Отмена", color = TopicMuted)
                }
            }
        )
    }
}

@Composable
private fun TopicListRow(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.84f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TopicBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 13.dp, end = 7.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE9EDFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TopicText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = TopicMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailing()
        }
    }
}

@Composable
private fun GroupTopicEditDialog(
    initialTitle: String,
    initialIcon: String,
    isBusy: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var topicTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var topicIcon by remember(initialIcon) { mutableStateOf(initialIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = topicIcon,
                    onValueChange = { topicIcon = it.take(8) },
                    label = { Text("Значок или эмодзи") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topicTitle,
                    onValueChange = { topicTitle = it.take(48) },
                    label = { Text("Название темы") },
                    placeholder = { Text("Например, Новости") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && topicTitle.trim().length in 2..48,
                onClick = { onSave(topicTitle, topicIcon) }
            ) {
                Text(if (isBusy) "Сохраняем…" else "Сохранить", color = TopicAccent)
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) {
                Text("Отмена", color = TopicMuted)
            }
        }
    )
}
