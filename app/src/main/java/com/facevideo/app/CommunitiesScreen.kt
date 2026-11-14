package com.facevideo.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facevideo.app.data.FaceVideoChannel
import com.facevideo.app.data.FaceVideoChannelEntry
import com.facevideo.app.data.FaceVideoChannelPost
import com.facevideo.app.data.FaceVideoChannelMemberProfile
import com.facevideo.app.data.FaceVideoChatSummary
import com.facevideo.app.data.FaceVideoFriendship
import com.facevideo.app.data.FaceVideoProfile
import com.facevideo.app.data.SupabaseChatRepository
import kotlinx.coroutines.launch

private val CommunityAccent = Color(0xFF5267DB)
private val CommunityAccentDark = Color(0xFF4053C7)
private val CommunityText = Color(0xFF202936)
private val CommunityMuted = Color(0xFF788493)
private val CommunitySurface = Color(0xEFFFFFFF)
private val CommunitySoft = Color(0xFFE9EDFF)
private val CommunityBorder = Color(0xFFE2E7F0)
private val CommunityDanger = Color(0xFFB65E6C)

private enum class CommunitySection(val title: String) {
    GROUPS("Группы"),
    CHANNELS("Каналы")
}

private enum class ChannelDirectoryFilter(val title: String) {
    ALL("Все"),
    SUBSCRIBED("Подписки"),
    OWNED("Мои")
}

@Composable
internal fun GroupsScreen(
    repository: SupabaseChatRepository,
    userId: String,
    onOpenGroup: (FaceVideoChatSummary) -> Unit
) {
    var section by remember { mutableStateOf(CommunitySection.GROUPS) }
    var groupChats by remember(userId) { mutableStateOf<List<FaceVideoChatSummary>>(emptyList()) }
    var channels by remember(userId) { mutableStateOf<List<FaceVideoChannelEntry>>(emptyList()) }
    var isLoading by remember(userId) { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember(userId) { mutableStateOf(0) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showCreateChannel by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<FaceVideoChannel?>(null) }
    var channelFilter by remember(userId) { mutableStateOf(ChannelDirectoryFilter.ALL) }
    var channelQuery by remember(userId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = selectedChannel != null) {
        selectedChannel = null
    }

    LaunchedEffect(repository, userId, refreshKey) {
        isLoading = true
        errorMessage = null
        try {
            val loadedChats = repository.listMyChats(userId)
            groupChats = loadedChats.filter { it.isGroup }
            channels = repository.listChannels(userId)
        } catch (error: Exception) {
            errorMessage = error.message ?: "Не удалось загрузить сообщества."
        } finally {
            isLoading = false
        }
    }

    val visibleChannels = remember(channels, channelFilter, channelQuery, userId) {
        val filtered = channels.filter { entry ->
            val isOwner = entry.channel.createdBy == userId
            val matchesFilter = when (channelFilter) {
                ChannelDirectoryFilter.ALL -> true
                ChannelDirectoryFilter.SUBSCRIBED -> entry.isSubscribed && !isOwner
                ChannelDirectoryFilter.OWNED -> isOwner
            }
            val query = channelQuery.trim()
            matchesFilter && (
                query.isBlank() ||
                    entry.channel.title.contains(query, ignoreCase = true) ||
                    entry.channel.description.contains(query, ignoreCase = true)
                )
        }
        filtered
    }

    if (selectedChannel != null) {
        ChannelDetailsScreen(
            repository = repository,
            userId = userId,
            channel = selectedChannel!!,
            isSubscribed = channels.firstOrNull { it.channel.id == selectedChannel!!.id }?.isSubscribed == true,
            onBack = { selectedChannel = null },
            onMembershipChanged = { refreshKey++ }
        )
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 13.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "ARTURCHAT",
                        color = CommunityAccentDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Сообщества",
                        color = CommunityText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Групповые чаты и каналы для публикаций",
                        color = CommunityMuted,
                        fontSize = 11.sp
                    )
                }
                Box {
                    Box(
                        Modifier.clip(RoundedCornerShape(15.dp))
                            .background(CommunityAccent)
                            .clickable { showCreateMenu = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("+ Создать", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = showCreateMenu,
                        onDismissRequest = { showCreateMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Создать группу") },
                            onClick = {
                                showCreateMenu = false
                                showCreateGroup = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Создать канал") },
                            onClick = {
                                showCreateMenu = false
                                showCreateChannel = true
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xAFFFFFFF))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CommunitySection.values().forEach { item ->
                    val selected = item == section
                    Box(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) CommunityAccent else Color.Transparent)
                            .clickable { section = item }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val count = if (item == CommunitySection.GROUPS) groupChats.size else channels.size
                        Text(
                            "${item.title} · $count",
                            color = if (selected) Color.White else CommunityText,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CommunityAccent)
                        Spacer(Modifier.height(12.dp))
                        Text("Загружаем сообщества…", color = CommunityMuted, fontSize = 12.sp)
                    }
                }
            } else if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage.orEmpty(), color = CommunityDanger, fontSize = 12.sp, textAlign = TextAlign.Center)
                        TextButton(onClick = { refreshKey++ }) { Text("Повторить", color = CommunityAccent) }
                    }
                }
            } else if (section == CommunitySection.GROUPS) {
                if (groupChats.isEmpty()) {
                    CommunityEmptyState(
                        icon = "◎",
                        title = "Пока нет групп",
                        description = "Создай группу, выбери друзей и начни общаться вместе.",
                        action = "Создать группу",
                        onAction = { showCreateGroup = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupChats, key = { it.id }) { chat ->
                            CommunityGroupRow(chat = chat, onClick = { onOpenGroup(chat) })
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xAFFFFFFF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChannelDirectoryFilter.values().forEach { filter ->
                        val selected = filter == channelFilter
                        Box(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { channelFilter = filter }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                filter.title,
                                color = if (selected) CommunityAccentDark else CommunityMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = channelQuery,
                    onValueChange = { channelQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp),
                    singleLine = true,
                    placeholder = { Text("Поиск каналов", fontSize = 12.sp) },
                    shape = RoundedCornerShape(14.dp)
                )
                if (visibleChannels.isEmpty()) {
                    when {
                        channels.isEmpty() -> CommunityEmptyState(
                            icon = "◉",
                            title = "Каналов пока нет",
                            description = "Создай канал, чтобы публиковать новости, фотографии и объявления для подписчиков.",
                            action = "Создать канал",
                            onAction = { showCreateChannel = true }
                        )
                        channelQuery.isNotBlank() -> CommunityEmptyState(
                            icon = "⌕",
                            title = "Ничего не найдено",
                            description = "Попробуй изменить поисковый запрос.",
                            action = "Очистить поиск",
                            onAction = { channelQuery = "" }
                        )
                        channelFilter == ChannelDirectoryFilter.OWNED -> CommunityEmptyState(
                            icon = "✦",
                            title = "У тебя пока нет каналов",
                            description = "Создай свой канал и публикуй новости и важные обновления.",
                            action = "Создать канал",
                            onAction = { showCreateChannel = true }
                        )
                        channelFilter == ChannelDirectoryFilter.SUBSCRIBED -> CommunityEmptyState(
                            icon = "◉",
                            title = "Нет подписок",
                            description = "Переключись на «Все», чтобы найти интересные каналы.",
                            action = "Все каналы",
                            onAction = { channelFilter = ChannelDirectoryFilter.ALL }
                        )
                        else -> CommunityEmptyState(
                            icon = "⌕",
                            title = "Ничего не найдено",
                            description = "Попробуй изменить поисковый запрос.",
                            action = "Очистить поиск",
                            onAction = { channelQuery = "" }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleChannels, key = { it.channel.id }) { entry ->
                            CommunityChannelRow(
                                entry = entry,
                                userId = userId,
                                onOpen = { selectedChannel = entry.channel },
                                onJoin = {
                                    scope.launch {
                                        runCatching { repository.joinChannel(entry.channel.id, userId) }
                                            .onSuccess { refreshKey++ }
                                            .onFailure { errorMessage = it.message ?: "Не удалось подписаться на канал." }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroup) {
        CreateCommunityGroupDialog(
            repository = repository,
            userId = userId,
            onDismiss = { showCreateGroup = false },
            onCreated = { chat ->
                showCreateGroup = false
                refreshKey++
                onOpenGroup(chat)
            }
        )
    }

    if (showCreateChannel) {
        CreateCommunityChannelDialog(
            repository = repository,
            onDismiss = { showCreateChannel = false },
            onCreated = { channel ->
                showCreateChannel = false
                refreshKey++
                selectedChannel = channel
            }
        )
    }
}

@Composable
private fun CommunityGroupRow(
    chat: FaceVideoChatSummary,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(CommunitySurface)
            .border(1.dp, CommunityBorder.copy(alpha = 0.7f), RoundedCornerShape(19.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArturChatTypeAvatar(ArturChatIconType.GROUP, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                chat.title.ifBlank { "Групповая переписка" },
                color = CommunityText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chat.lastMessagePreview.ifBlank { "Групповой чат · Нажми, чтобы открыть" },
                color = CommunityMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("›", color = CommunityAccent, fontSize = 24.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun CommunityChannelRow(
    entry: FaceVideoChannelEntry,
    userId: String,
    onOpen: () -> Unit,
    onJoin: () -> Unit
) {
    val isOwner = entry.channel.createdBy == userId
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(CommunitySurface)
            .border(1.dp, CommunityBorder.copy(alpha = 0.7f), RoundedCornerShape(19.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArturChatTypeAvatar(ArturChatIconType.CHANNEL, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.channel.title,
                color = CommunityText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                entry.channel.description.ifBlank { "Канал ArturChat" },
                color = CommunityMuted,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isOwner) {
            Text("Мой канал", color = CommunityAccentDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        } else if (entry.isSubscribed) {
            Text("Открыть", color = CommunityAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(CommunitySoft)
                    .clickable(onClick = onJoin)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text("Подписаться", color = CommunityAccentDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CommunityInitial(
    icon: String,
    label: String
) {
    Box(
        Modifier.size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFFE9EDFF)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon.ifBlank { label.take(1).uppercase() },
            color = CommunityAccent,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CommunityEmptyState(
    icon: String,
    title: String,
    description: String,
    action: String,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(78.dp).clip(CircleShape).background(CommunitySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, color = CommunityAccent, fontSize = 31.sp)
            }
            Spacer(Modifier.height(17.dp))
            Text(title, color = CommunityText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(description, color = CommunityMuted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction) {
                Text(action, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CreateCommunityGroupDialog(
    repository: SupabaseChatRepository,
    userId: String,
    onDismiss: () -> Unit,
    onCreated: (FaceVideoChatSummary) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<FaceVideoProfile>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId) {
        try {
            val relationships = repository.listMyFriendships(userId)
            val friendIds = relationships
                .filter { it.status == "accepted" }
                .map { if (it.userLow == userId) it.userHigh else it.userLow }
                .distinct()
            friends = friendIds.mapNotNull { id -> repository.getProfile(id) }
                .sortedBy { it.displayName.ifBlank { it.username }.lowercase() }
        } catch (error: Exception) {
            errorMessage = error.message ?: "Не удалось загрузить список друзей."
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = Color(0xFFF7F8FF),
        title = { Text("Создать группу", color = CommunityText, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Дай группе название и выбери друзей, которых хочешь пригласить.", color = CommunityMuted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название группы") }
                )
                Spacer(Modifier.height(12.dp))
                Text("УЧАСТНИКИ · ${selectedIds.size}", color = CommunityAccentDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                when {
                    isLoading -> CircularProgressIndicator(color = CommunityAccent, modifier = Modifier.size(26.dp))
                    friends.isEmpty() -> Text("Пока нет друзей для добавления. Сначала добавь друзей в разделе «Друзья».", color = CommunityMuted, fontSize = 11.sp)
                    else -> friends.forEach { profile ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selectedIds = if (profile.id in selectedIds) selectedIds - profile.id else selectedIds + profile.id
                            }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = profile.id in selectedIds,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + profile.id else selectedIds - profile.id
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(profile.displayName.ifBlank { profile.username }, color = CommunityText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("@${profile.username}", color = CommunityMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage.orEmpty(), color = CommunityDanger, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && !isLoading && title.trim().length >= 2 && selectedIds.isNotEmpty(),
                onClick = {
                    scope.launch {
                        isBusy = true
                        errorMessage = null
                        runCatching {
                            val chatId = repository.createGroupChat(selectedIds.toList(), title)
                            repository.getChat(chatId)
                                ?: throw IllegalStateException("Группа создана, но не удалось открыть её.")
                        }.onSuccess(onCreated)
                            .onFailure { errorMessage = it.message ?: "Не удалось создать группу." }
                        isBusy = false
                    }
                }
            ) {
                Text(if (isBusy) "Создаём…" else "Создать", color = CommunityAccent)
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) { Text("Отмена", color = CommunityMuted) }
        }
    )
}

@Composable
private fun CreateCommunityChannelDialog(
    repository: SupabaseChatRepository,
    onDismiss: () -> Unit,
    onCreated: (FaceVideoChannel) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = Color(0xFFF7F8FF),
        title = { Text("Создать канал", color = CommunityText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Канал — место для публикаций. Подписчики смогут читать записи, а публиковать их будет владелец.", color = CommunityMuted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название канала") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    label = { Text("Описание") }
                )
                Spacer(Modifier.height(7.dp))
                Text("Канал будет публичным и появится в общем каталоге.", color = CommunityMuted, fontSize = 10.sp)
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage.orEmpty(), color = CommunityDanger, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && title.trim().length >= 2,
                onClick = {
                    scope.launch {
                        isBusy = true
                        errorMessage = null
                        runCatching { repository.createChannel(title, description, true) }
                            .onSuccess(onCreated)
                            .onFailure { errorMessage = it.message ?: "Не удалось создать канал." }
                        isBusy = false
                    }
                }
            ) {
                Text(if (isBusy) "Создаём…" else "Создать канал", color = CommunityAccent)
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) { Text("Отмена", color = CommunityMuted) }
        }
    )
}

@Composable
private fun ChannelDetailsScreen(
    repository: SupabaseChatRepository,
    userId: String,
    channel: FaceVideoChannel,
    isSubscribed: Boolean,
    onBack: () -> Unit,
    onMembershipChanged: () -> Unit
) {
    var currentChannel by remember(channel.id) { mutableStateOf(channel) }
    var subscribed by remember(channel.id, isSubscribed) { mutableStateOf(isSubscribed) }
    var posts by remember(channel.id) { mutableStateOf<List<FaceVideoChannelPost>>(emptyList()) }
    var members by remember(channel.id) { mutableStateOf<List<FaceVideoChannelMemberProfile>>(emptyList()) }
    var isLoading by remember(channel.id) { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var draft by remember(channel.id) { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showManageMenu by remember { mutableStateOf(false) }
    var showEditChannel by remember { mutableStateOf(false) }
    var showDeleteChannelConfirmation by remember { mutableStateOf(false) }
    var showSubscribers by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<FaceVideoChannelPost?>(null) }
    var deletingPost by remember { mutableStateOf<FaceVideoChannelPost?>(null) }
    val scope = rememberCoroutineScope()
    val isOwner = currentChannel.createdBy == userId
    val subscribers = members.filter { it.member.role == "subscriber" }

    LaunchedEffect(repository, currentChannel.id, isOwner) {
        isLoading = true
        errorMessage = null
        runCatching { repository.listChannelPosts(currentChannel.id) }
            .onSuccess { posts = it }
            .onFailure { errorMessage = it.message ?: "Не удалось загрузить публикации." }
        if (isOwner) {
            runCatching { repository.listChannelMembers(currentChannel.id) }
                .onSuccess { members = it }
                .onFailure { errorMessage = it.message ?: "Не удалось загрузить список подписчиков." }
        }
        isLoading = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)) {
                Text("‹ Назад", color = CommunityAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    currentChannel.title,
                    color = CommunityText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Канал", color = CommunityMuted, fontSize = 9.sp)
            }
            Spacer(Modifier.weight(1f))
            Box {
                TextButton(
                    onClick = { showManageMenu = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                    enabled = isOwner || subscribed
                ) {
                    Text("⋯", color = CommunityAccentDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(
                    expanded = showManageMenu,
                    onDismissRequest = { showManageMenu = false }
                ) {
                    if (isOwner) {
                        DropdownMenuItem(
                            text = { Text("Изменить канал") },
                            onClick = {
                                showManageMenu = false
                                showEditChannel = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Подписчики · ${subscribers.size}") },
                            onClick = {
                                showManageMenu = false
                                showSubscribers = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить канал", color = CommunityDanger) },
                            onClick = {
                                showManageMenu = false
                                showDeleteChannelConfirmation = true
                            }
                        )
                    } else if (subscribed) {
                        DropdownMenuItem(
                            text = { Text("Отписаться") },
                            onClick = {
                                showManageMenu = false
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.leaveChannel(currentChannel.id, userId) }
                                        .onSuccess {
                                            subscribed = false
                                            onMembershipChanged()
                                        }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отменить подписку." }
                                    isBusy = false
                                }
                            }
                        )
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CommunitySurface)
                .border(1.dp, CommunityBorder, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArturChatTypeAvatar(ArturChatIconType.CHANNEL, Modifier.size(54.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        currentChannel.title,
                        color = CommunityText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        when {
                            isOwner -> "Ваш канал"
                            subscribed -> "Вы подписаны"
                            else -> "Публичный канал"
                        },
                        color = CommunityMuted,
                        fontSize = 10.sp
                    )
                }
            }
            if (currentChannel.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    currentChannel.description,
                    color = CommunityMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
            if (isOwner) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${subscribers.size} подписчиков",
                            color = CommunityText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Только владелец видит список подписчиков",
                            color = CommunityMuted,
                            fontSize = 9.sp
                        )
                    }
                    TextButton(onClick = { showSubscribers = true }) {
                        Text("Посмотреть", color = CommunityAccent, fontSize = 11.sp)
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            runCatching {
                                if (subscribed) repository.leaveChannel(currentChannel.id, userId)
                                else repository.joinChannel(currentChannel.id, userId)
                            }.onSuccess {
                                subscribed = !subscribed
                                onMembershipChanged()
                            }.onFailure {
                                errorMessage = it.message ?: "Не удалось изменить подписку."
                            }
                            isBusy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            isBusy -> "Обрабатываем…"
                            subscribed -> "Вы подписаны · Отписаться"
                            else -> "Подписаться на канал"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (isOwner) {
            Spacer(Modifier.height(11.dp))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CommunitySurface)
                    .border(1.dp, CommunityBorder.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Text("НОВАЯ ПУБЛИКАЦИЯ", color = CommunityAccentDark, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(4000) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    placeholder = { Text("Поделитесь новостью с подписчиками…", fontSize = 12.sp) }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        enabled = !isBusy && draft.isNotBlank(),
                        onClick = {
                            scope.launch {
                                isBusy = true
                                errorMessage = null
                                runCatching { repository.createChannelPost(currentChannel.id, userId, draft) }
                                    .onSuccess {
                                        posts = listOf(it) + posts
                                        draft = ""
                                    }
                                    .onFailure { errorMessage = it.message ?: "Не удалось опубликовать запись." }
                                isBusy = false
                            }
                        }
                    ) {
                        Text(if (isBusy) "Публикуем…" else "Опубликовать", color = CommunityAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(13.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ПУБЛИКАЦИИ",
                color = CommunityAccentDark,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.weight(1f))
            Text(posts.size.toString(), color = CommunityMuted, fontSize = 10.sp)
        }
        if (errorMessage != null) {
            Text(
                errorMessage.orEmpty(),
                modifier = Modifier.padding(horizontal = 19.dp, vertical = 5.dp),
                color = CommunityDanger,
                fontSize = 10.sp
            )
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                isLoading && posts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CommunityAccent)
                }
                posts.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isOwner) "В канале пока нет публикаций. Напиши первую запись выше."
                        else "В этом канале пока нет публикаций.",
                        color = CommunityMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        ChannelPostCard(
                            post = post,
                            canManage = isOwner,
                            onEdit = { editingPost = post },
                            onDelete = { deletingPost = post }
                        )
                    }
                }
            }
        }
    }

    if (showEditChannel) {
        EditCommunityChannelDialog(
            channel = currentChannel,
            onDismiss = { showEditChannel = false },
            onSave = { title, description ->
                scope.launch {
                    isBusy = true
                    errorMessage = null
                    runCatching { repository.updateChannel(currentChannel.id, userId, title, description) }
                        .onSuccess {
                            currentChannel = it
                            showEditChannel = false
                            onMembershipChanged()
                        }
                        .onFailure { errorMessage = it.message ?: "Не удалось сохранить канал." }
                    isBusy = false
                }
            },
            isBusy = isBusy
        )
    }

    if (showDeleteChannelConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showDeleteChannelConfirmation = false },
            containerColor = Color.White,
            title = { Text("Удалить канал?", color = CommunityText, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Канал «${currentChannel.title}» и все его публикации будут удалены. Подписчики больше не увидят этот канал.",
                    color = CommunityMuted,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            runCatching { repository.deleteChannel(currentChannel.id, userId) }
                                .onSuccess {
                                    showDeleteChannelConfirmation = false
                                    onBack()
                                    onMembershipChanged()
                                }
                                .onFailure {
                                    errorMessage = it.message ?: "Не удалось удалить канал."
                                    showDeleteChannelConfirmation = false
                                }
                            isBusy = false
                        }
                    }
                ) {
                    Text(if (isBusy) "Удаляем…" else "Удалить канал", color = CommunityDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChannelConfirmation = false }, enabled = !isBusy) {
                    Text("Отмена", color = CommunityMuted)
                }
            }
        )
    }

    if (showSubscribers) {
        ChannelSubscribersDialog(
            members = subscribers,
            onDismiss = { showSubscribers = false }
        )
    }

    if (editingPost != null) {
        EditChannelPostDialog(
            post = editingPost!!,
            isBusy = isBusy,
            onDismiss = { editingPost = null },
            onSave = { body ->
                val post = editingPost!!
                scope.launch {
                    isBusy = true
                    runCatching { repository.updateChannelPost(currentChannel.id, post.id, body) }
                        .onSuccess { updated ->
                            posts = posts.map { if (it.id == updated.id) updated else it }
                            editingPost = null
                        }
                        .onFailure { errorMessage = it.message ?: "Не удалось изменить публикацию." }
                    isBusy = false
                }
            }
        )
    }

    if (deletingPost != null) {
        val post = deletingPost!!
        AlertDialog(
            onDismissRequest = { if (!isBusy) deletingPost = null },
            containerColor = Color.White,
            title = { Text("Удалить публикацию?", color = CommunityText, fontWeight = FontWeight.Bold) },
            text = { Text("Эта запись исчезнет из канала у всех подписчиков.", color = CommunityMuted, fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            runCatching { repository.deleteChannelPost(currentChannel.id, post.id) }
                                .onSuccess {
                                    posts = posts.filterNot { it.id == post.id }
                                    deletingPost = null
                                }
                                .onFailure { errorMessage = it.message ?: "Не удалось удалить публикацию." }
                            isBusy = false
                        }
                    }
                ) { Text(if (isBusy) "Удаляем…" else "Удалить", color = CommunityDanger) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPost = null }, enabled = !isBusy) {
                    Text("Отмена", color = CommunityMuted)
                }
            }
        )
    }
}

@Composable
private fun EditCommunityChannelDialog(
    channel: FaceVideoChannel,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    isBusy: Boolean
) {
    var title by remember(channel.id) { mutableStateOf(channel.title) }
    var description by remember(channel.id) { mutableStateOf(channel.description) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = Color.White,
        title = { Text("Настройки канала", color = CommunityText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название канала") }
                )
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    label = { Text("Описание") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && title.trim().length >= 2,
                onClick = { onSave(title, description) }
            ) { Text(if (isBusy) "Сохраняем…" else "Сохранить", color = CommunityAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Отмена", color = CommunityMuted)
            }
        }
    )
}

@Composable
private fun EditChannelPostDialog(
    post: FaceVideoChannelPost,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var body by remember(post.id) { mutableStateOf(post.body) }
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = Color.White,
        title = { Text("Редактировать публикацию", color = CommunityText, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(4000) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                label = { Text("Текст публикации") }
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && body.trim().isNotBlank(),
                onClick = { onSave(body) }
            ) { Text(if (isBusy) "Сохраняем…" else "Сохранить", color = CommunityAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Отмена", color = CommunityMuted)
            }
        }
    )
}

@Composable
private fun ChannelSubscribersDialog(
    members: List<FaceVideoChannelMemberProfile>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column {
                Text("Подписчики", color = CommunityText, fontWeight = FontWeight.Bold)
                Text("${members.size} человек", color = CommunityMuted, fontSize = 10.sp)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())
            ) {
                if (members.isEmpty()) {
                    Text("Пока никто не подписался на канал.", color = CommunityMuted, fontSize = 12.sp)
                } else {
                    members.forEach { entry ->
                        val profile = entry.profile
                        val name = profile?.let { it.displayName.ifBlank { it.username } } ?: "Пользователь"
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CommunityInitial(icon = name.take(1).uppercase(), label = name)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, color = CommunityText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    profile?.username?.let { "@$it" } ?: "Подписчик",
                                    color = CommunityMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Text("Подписчик", color = CommunityAccentDark, fontSize = 9.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово", color = CommunityAccent) }
        }
    )
}

@Composable
private fun ChannelPostCard(
    post: FaceVideoChannelPost,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember(post.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CommunitySurface)
            .border(1.dp, CommunityBorder.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArturChatTypeAvatar(ArturChatIconType.CHANNEL, Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Публикация канала", color = CommunityText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    post.createdAt.replace("T", " ").take(16) +
                        if (post.editedAt != null) " · изменено" else "",
                    color = CommunityMuted,
                    fontSize = 9.sp
                )
            }
            if (canManage) {
                Box {
                    Text(
                        "⋯",
                        modifier = Modifier.clickable { showMenu = true }.padding(horizontal = 6.dp),
                        color = CommunityAccent,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить", color = CommunityDanger) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        Text(post.body, color = CommunityText, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
