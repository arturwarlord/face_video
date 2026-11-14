package com.facevideo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facevideo.app.data.FaceVideoChannel
import com.facevideo.app.data.FaceVideoChannelMemberProfile
import com.facevideo.app.data.FaceVideoProfile
import com.facevideo.app.data.SupabaseChatRepository
import kotlinx.coroutines.launch

private val ChannelPanelAccent = Color(0xFF5369DC)
private val ChannelPanelAccentDark = Color(0xFF4054C5)
private val ChannelPanelText = Color(0xFF202936)
private val ChannelPanelMuted = Color(0xFF7A8796)
private val ChannelPanelBorder = Color(0xFFE2E7F0)
private val ChannelPanelSoft = Color(0xFFE9EDFF)
private val ChannelPanelDanger = Color(0xFFB45F6D)

@Composable
internal fun ChannelManagementScreen(
    repository: SupabaseChatRepository,
    userId: String,
    channel: FaceVideoChannel,
    onDismiss: () -> Unit,
    onEditChannel: () -> Unit,
    onChannelChanged: () -> Unit
) {
    var members by remember(channel.id) { mutableStateOf<List<FaceVideoChannelMemberProfile>>(emptyList()) }
    var isLoading by remember(channel.id) { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var refreshKey by remember(channel.id) { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var permissionsTarget by remember { mutableStateOf<FaceVideoChannelMemberProfile?>(null) }
    var removalTarget by remember { mutableStateOf<FaceVideoChannelMemberProfile?>(null) }
    var avatarPreview by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, channel.id, refreshKey) {
        isLoading = true
        errorMessage = null
        runCatching { repository.listChannelMembers(channel.id) }
            .onSuccess { members = it }
            .onFailure { errorMessage = it.message ?: "Не удалось загрузить участников канала." }
        isLoading = false
    }

    val myMembership = members.firstOrNull { it.member.userId == userId }?.member
    val isOwner = channel.createdBy == userId || myMembership?.role == "owner"
    val isAdmin = myMembership?.role == "admin"
    val canChangeInfo = isOwner || (isAdmin && myMembership?.canChangeInfo == true)
    val canInvite = isOwner || (isAdmin && myMembership?.canInviteUsers == true)
    val canRemove = isOwner || (isAdmin && myMembership?.canRemoveUsers == true)
    val canPublish = isOwner || (isAdmin && myMembership?.canPublishPosts == true)
    val subscribers = members.filter { it.member.role == "subscriber" }
    val administrators = members.filter { it.member.role == "owner" || it.member.role == "admin" }

    fun refresh() {
        refreshKey += 1
        onChannelChanged()
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFFE2DCFF),
                    Color(0xFFE2E9FF),
                    Color(0xFFE8F0FF),
                    Color(0xFFF2F5FF)
                )
            )
        )
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier.align(Alignment.CenterStart),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Text("‹ Назад", color = ChannelPanelAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Участники канала",
                modifier = Modifier.align(Alignment.Center),
                color = ChannelPanelText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading && members.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ChannelPanelAccent)
            }
        } else {
            Column(
                Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(90.dp)) {
                        if (!channel.avatarUrl.isNullOrBlank()) {
                            Avatar(
                                initials = channel.title.take(1).uppercase().ifBlank { "К" },
                                style = 2,
                                modifier = Modifier.fillMaxSize(),
                                imageUrl = channel.avatarUrl,
                                imageCacheKey = "channel:${channel.id}",
                                onClick = { avatarPreview = true }
                            )
                        } else {
                            ArturChatTypeAvatar(ArturChatIconType.CHANNEL, Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        channel.title,
                        color = ChannelPanelText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${subscribers.size} подписчиков · ${administrators.size} администраторов",
                        color = ChannelPanelMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    if (channel.description.isNotBlank()) {
                        Spacer(Modifier.height(9.dp))
                        Text(
                            channel.description,
                            color = ChannelPanelMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canInvite) {
                            ChannelPanelAction(
                                label = "Добавить людей",
                                modifier = Modifier.weight(1f),
                                onClick = { showInviteDialog = true }
                            )
                        }
                        if (canChangeInfo) {
                            ChannelPanelAction(
                                label = "Изменить канал",
                                modifier = Modifier.weight(1f),
                                onClick = onEditChannel
                            )
                        }
                    }
                }

                ChannelPanelSectionTitle("РУКОВОДСТВО", administrators.size)
                Spacer(Modifier.height(7.dp))
                if (administrators.isEmpty()) {
                    Text("Владелец канала не найден.", color = ChannelPanelMuted, fontSize = 11.sp)
                } else {
                    administrators.forEach { entry ->
                        ChannelMemberRow(
                            entry = entry,
                            userId = userId,
                            canManage = isOwner && entry.member.userId != userId,
                            isBusy = isBusy,
                            onManagePermissions = { permissionsTarget = entry },
                            onDemote = {
                                scope.launch {
                                    isBusy = true
                                    errorMessage = null
                                    runCatching {
                                        repository.setChannelMemberRole(channel.id, entry.member.userId, "subscriber")
                                    }.onSuccess {
                                        infoMessage = "Права администратора сняты."
                                        refresh()
                                    }.onFailure {
                                        errorMessage = it.message ?: "Не удалось изменить роль."
                                    }
                                    isBusy = false
                                }
                            },
                            onRemove = { removalTarget = entry },
                            onPromote = {}
                        )
                    }
                }

                Spacer(Modifier.height(17.dp))
                ChannelPanelSectionTitle("ПОДПИСЧИКИ", subscribers.size)
                Spacer(Modifier.height(7.dp))
                when {
                    subscribers.isEmpty() -> Text(
                        "Пока нет подписчиков. Добавь друзей, чтобы начать развивать канал.",
                        color = ChannelPanelMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    else -> subscribers.forEach { entry ->
                        ChannelMemberRow(
                            entry = entry,
                            userId = userId,
                            canManage = canRemove && entry.member.userId != userId,
                            isBusy = isBusy,
                            onManagePermissions = {},
                            onDemote = {},
                            onRemove = { removalTarget = entry },
                            onPromote = {
                                if (isOwner) {
                                    scope.launch {
                                        isBusy = true
                                        errorMessage = null
                                        runCatching {
                                            repository.setChannelMemberRole(channel.id, entry.member.userId, "admin")
                                        }.onSuccess {
                                            infoMessage = "Участник назначен администратором."
                                            refresh()
                                        }.onFailure {
                                            errorMessage = it.message ?: "Не удалось назначить администратора."
                                        }
                                        isBusy = false
                                    }
                                }
                            }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(errorMessage.orEmpty(), color = ChannelPanelDanger, fontSize = 11.sp)
                }
                if (infoMessage != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(infoMessage.orEmpty(), color = ChannelPanelAccentDark, fontSize = 11.sp)
                }
                if (isOwner) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Администраторы могут получать отдельные права на управление каналом.",
                        color = ChannelPanelMuted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (avatarPreview && !channel.avatarUrl.isNullOrBlank()) {
        FullscreenAvatarDialog(
            imageUrl = channel.avatarUrl!!,
            imageCacheKey = "channel:${channel.id}",
            title = channel.title,
            onDismiss = { avatarPreview = false }
        )
    }

    if (showInviteDialog) {
        InviteChannelMembersDialog(
            repository = repository,
            userId = userId,
            channelId = channel.id,
            currentMemberIds = members.mapTo(mutableSetOf()) { it.member.userId },
            isBusy = isBusy,
            onDismiss = { showInviteDialog = false },
            onInvite = { count ->
                showInviteDialog = false
                infoMessage = if (count > 0) "Добавлено участников: $count" else "Новые участники не добавлены."
                refresh()
            },
            onError = { errorMessage = it }
        )
    }

    if (permissionsTarget != null) {
        ChannelAdminPermissionsDialog(
            member = permissionsTarget!!,
            isBusy = isBusy,
            onDismiss = { permissionsTarget = null },
            onSave = { changeInfo, inviteUsers, removeUsers, publishPosts ->
                val target = permissionsTarget!!
                scope.launch {
                    isBusy = true
                    errorMessage = null
                    runCatching {
                        repository.setChannelAdminPermissions(
                            channel.id,
                            target.member.userId,
                            changeInfo,
                            inviteUsers,
                            removeUsers,
                            publishPosts
                        )
                    }.onSuccess {
                        permissionsTarget = null
                        infoMessage = "Права администратора сохранены."
                        refresh()
                    }.onFailure {
                        errorMessage = it.message ?: "Не удалось сохранить права администратора."
                    }
                    isBusy = false
                }
            }
        )
    }

    if (removalTarget != null) {
        val target = removalTarget!!
        val name = target.profile?.displayName?.ifBlank { target.profile.username } ?: "участника"
        AlertDialog(
            onDismissRequest = { if (!isBusy) removalTarget = null },
            containerColor = Color.White,
            title = { Text("Удалить из канала?", color = ChannelPanelText, fontWeight = FontWeight.Bold) },
            text = { Text("$name больше не сможет просматривать публикации этого канала через подписку.", color = ChannelPanelMuted, fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            runCatching { repository.removeChannelMember(channel.id, target.member.userId) }
                                .onSuccess {
                                    removalTarget = null
                                    infoMessage = "Участник удалён из канала."
                                    refresh()
                                }
                                .onFailure { errorMessage = it.message ?: "Не удалось удалить участника." }
                            isBusy = false
                        }
                    }
                ) { Text(if (isBusy) "Удаляем…" else "Удалить", color = ChannelPanelDanger) }
            },
            dismissButton = {
                TextButton(onClick = { removalTarget = null }, enabled = !isBusy) {
                    Text("Отмена", color = ChannelPanelMuted)
                }
            }
        )
    }
}

@Composable
private fun ChannelMemberRow(
    entry: FaceVideoChannelMemberProfile,
    userId: String,
    canManage: Boolean,
    isBusy: Boolean,
    onManagePermissions: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit,
    onPromote: () -> Unit
) {
    var menuExpanded by remember(entry.member.userId) { mutableStateOf(false) }
    var avatarPreview by remember(entry.member.userId) { mutableStateOf(false) }
    val profile = entry.profile
    val name = profile?.let { it.displayName.ifBlank { it.username } } ?: "Пользователь"
    val username = profile?.username?.let { "@$it" } ?: entry.member.userId.take(8)

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.56f))
            .border(1.dp, ChannelPanelBorder.copy(alpha = 0.72f), RoundedCornerShape(17.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            initials = name.take(1).uppercase().ifBlank { "?" },
            style = 2,
            modifier = Modifier.size(42.dp),
            imageUrl = profile?.avatarUrl,
            imageCacheKey = profile?.id,
            onClick = { if (!profile?.avatarUrl.isNullOrBlank()) avatarPreview = true }
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name + if (entry.member.userId == userId) " · вы" else "",
                color = ChannelPanelText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(username, color = ChannelPanelMuted, fontSize = 9.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when (entry.member.role) {
                    "owner" -> "Владелец"
                    "admin" -> "Администратор"
                    else -> "Подписчик"
                },
                color = if (entry.member.role == "owner" || entry.member.role == "admin") ChannelPanelAccentDark else ChannelPanelMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (canManage) {
                Box {
                    Text(
                        "⋯",
                        modifier = Modifier.clickable(enabled = !isBusy) { menuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = ChannelPanelAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (entry.member.role == "admin") {
                            DropdownMenuItem(
                                text = { Text("Права администратора") },
                                onClick = { menuExpanded = false; onManagePermissions() }
                            )
                            DropdownMenuItem(
                                text = { Text("Снять права администратора") },
                                onClick = { menuExpanded = false; onDemote() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Назначить администратором") },
                                onClick = { menuExpanded = false; onPromote() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Удалить из канала", color = ChannelPanelDanger) },
                            onClick = { menuExpanded = false; onRemove() }
                        )
                    }
                }
            }
        }
    }

    if (avatarPreview && !profile?.avatarUrl.isNullOrBlank() && profile?.id != null) {
        FullscreenAvatarDialog(
            imageUrl = profile!!.avatarUrl!!,
            imageCacheKey = profile!!.id,
            title = name,
            onDismiss = { avatarPreview = false }
        )
    }
}

@Composable
private fun ChannelPanelSectionTitle(title: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = ChannelPanelAccentDark, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Spacer(Modifier.weight(1f))
        Text(count.toString(), color = ChannelPanelMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ChannelPanelAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = ChannelPanelSoft,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(label, color = ChannelPanelAccentDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChannelAdminPermissionsDialog(
    member: FaceVideoChannelMemberProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val name = member.profile?.let { it.displayName.ifBlank { it.username } } ?: "Администратор"
    var changeInfo by remember(member.member.userId) { mutableStateOf(member.member.canChangeInfo) }
    var inviteUsers by remember(member.member.userId) { mutableStateOf(member.member.canInviteUsers) }
    var removeUsers by remember(member.member.userId) { mutableStateOf(member.member.canRemoveUsers) }
    var publishPosts by remember(member.member.userId) { mutableStateOf(member.member.canPublishPosts) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = Color.White,
        title = {
            Column {
                Text("Права администратора", color = ChannelPanelText, fontWeight = FontWeight.Bold)
                Text(name, color = ChannelPanelMuted, fontSize = 11.sp)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Выбери, какие действия сможет выполнять этот администратор.", color = ChannelPanelMuted, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(10.dp))
                ChannelPermissionToggle("Изменять информацию канала", "Название и описание канала", changeInfo, isBusy) { changeInfo = it }
                ChannelPermissionToggle("Добавлять участников", "Приглашать друзей в канал", inviteUsers, isBusy) { inviteUsers = it }
                ChannelPermissionToggle("Удалять подписчиков", "Удалять участников из канала", removeUsers, isBusy) { removeUsers = it }
                ChannelPermissionToggle("Публиковать записи", "Создавать и редактировать свои публикации", publishPosts, isBusy) { publishPosts = it }
            }
        },
        confirmButton = {
            TextButton(enabled = !isBusy, onClick = { onSave(changeInfo, inviteUsers, removeUsers, publishPosts) }) {
                Text(if (isBusy) "Сохраняем…" else "Сохранить", color = ChannelPanelAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Отмена", color = ChannelPanelMuted) }
        }
    )
}

@Composable
private fun ChannelPermissionToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ChannelPanelText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = ChannelPanelMuted, fontSize = 9.sp)
        }
        Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InviteChannelMembersDialog(
    repository: SupabaseChatRepository,
    userId: String,
    channelId: String,
    currentMemberIds: Set<String>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onInvite: (Int) -> Unit,
    onError: (String) -> Unit
) {
    var friends by remember(channelId, userId) { mutableStateOf<List<FaceVideoProfile>>(emptyList()) }
    var selectedIds by remember(channelId, userId) { mutableStateOf<Set<String>>(emptySet()) }
    var search by remember(channelId, userId) { mutableStateOf("") }
    var isLoading by remember(channelId, userId) { mutableStateOf(true) }
    var localBusy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId, channelId, currentMemberIds) {
        isLoading = true
        loadError = null
        runCatching {
            val friendIds = repository.listMyFriendships(userId)
                .filter { it.status == "accepted" }
                .map { if (it.userLow == userId) it.userHigh else it.userLow }
                .distinct()
                .filterNot { it in currentMemberIds || it == userId }
            repository.listProfiles(friendIds)
                .sortedBy { it.displayName.ifBlank { it.username }.lowercase() }
        }.onSuccess { friends = it }
            .onFailure { loadError = it.message ?: "Не удалось загрузить список друзей." }
        isLoading = false
    }

    val visibleFriends = friends.filter { profile ->
        val query = search.trim().removePrefix("@").lowercase()
        query.isBlank() ||
            profile.username.lowercase().contains(query) ||
            profile.displayName.lowercase().contains(query)
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy && !localBusy) onDismiss() },
        containerColor = Color.White,
        title = {
            Column {
                Text("Добавить участников", color = ChannelPanelText, fontWeight = FontWeight.Bold)
                Text("Выбери друзей для приглашения в канал.", color = ChannelPanelMuted, fontSize = 10.sp)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(48) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Поиск друзей") }
                )
                Spacer(Modifier.height(8.dp))
                if (actionError != null) {
                    Text(actionError.orEmpty(), color = ChannelPanelDanger, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                }
                when {
                    isLoading -> Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ChannelPanelAccent, modifier = Modifier.size(24.dp))
                    }
                    loadError != null -> Text(loadError.orEmpty(), color = ChannelPanelDanger, fontSize = 11.sp)
                    friends.isEmpty() -> Text("Все твои друзья уже подписаны на канал. Чтобы пригласить нового человека, сначала добавь его в друзья.", color = ChannelPanelMuted, fontSize = 11.sp)
                    visibleFriends.isEmpty() -> Text("Ничего не найдено.", color = ChannelPanelMuted, fontSize = 11.sp)
                    else -> Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        Text("ДРУЗЬЯ · ${selectedIds.size} ВЫБРАНО", color = ChannelPanelAccentDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        visibleFriends.forEach { profile ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    selectedIds = if (profile.id in selectedIds) selectedIds - profile.id else selectedIds + profile.id
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(
                                    initials = profile.displayName.ifBlank { profile.username }.take(1).uppercase(),
                                    style = 2,
                                    modifier = Modifier.size(36.dp),
                                    imageUrl = profile.avatarUrl,
                                    imageCacheKey = profile.id
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(profile.displayName.ifBlank { profile.username }, color = ChannelPanelText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("@${profile.username}", color = ChannelPanelMuted, fontSize = 9.sp)
                                }
                                Checkbox(
                                    checked = profile.id in selectedIds,
                                    enabled = !isBusy && !localBusy,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + profile.id else selectedIds - profile.id
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && !localBusy && !isLoading && selectedIds.isNotEmpty(),
                onClick = {
                    scope.launch {
                        localBusy = true
                        actionError = null
                        runCatching { repository.addChannelMembers(channelId, selectedIds.toList()) }
                            .onSuccess(onInvite)
                            .onFailure {
                                val message = it.message ?: "Не удалось добавить участников."
                                actionError = message
                                onError(message)
                            }
                        localBusy = false
                    }
                }
            ) { Text(if (localBusy) "Добавляем…" else "Добавить", color = ChannelPanelAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy && !localBusy) {
                Text("Отмена", color = ChannelPanelMuted)
            }
        }
    )
}
