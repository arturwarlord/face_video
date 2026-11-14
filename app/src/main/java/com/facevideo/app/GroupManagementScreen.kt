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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.facevideo.app.data.FaceVideoGroup
import com.facevideo.app.data.FaceVideoGroupMemberProfile
import com.facevideo.app.data.FaceVideoProfile
import com.facevideo.app.data.SupabaseChatRepository
import kotlinx.coroutines.launch

private val GroupPanelAccent = Color(0xFF5369DC)
private val GroupPanelAccentDark = Color(0xFF4054C5)
private val GroupPanelText = Color(0xFF202936)
private val GroupPanelMuted = Color(0xFF7A8796)
private val GroupPanelBorder = Color(0xFFE2E7F0)
private val GroupPanelSoft = Color(0xFFE9EDFF)
private val GroupPanelDanger = Color(0xFFB45F6D)
private val GroupPanelBackground = Color(0xFFF7F8FC)

@Composable
internal fun GroupManagementDialog(
    repository: SupabaseChatRepository,
    userId: String,
    chatId: String,
    onDismiss: () -> Unit,
    onGroupUpdated: (String) -> Unit,
    onGroupClosed: () -> Unit
) {
    var group by remember(chatId) { mutableStateOf<FaceVideoGroup?>(null) }
    var members by remember(chatId) { mutableStateOf<List<FaceVideoGroupMemberProfile>>(emptyList()) }
    var isLoading by remember(chatId) { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var refreshKey by remember(chatId) { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<FaceVideoGroupMemberProfile?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, chatId, refreshKey) {
        isLoading = true
        errorMessage = null
        runCatching {
            val loadedGroup = repository.getGroupDetails(chatId)
                ?: throw IllegalStateException("Группа не найдена.")
            val loadedMembers = repository.listGroupMembers(chatId)
            loadedGroup to loadedMembers
        }.onSuccess { (loadedGroup, loadedMembers) ->
            group = loadedGroup
            members = loadedMembers
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить информацию о группе."
        }
        isLoading = false
    }

    val myMembership = members.firstOrNull { it.member.userId == userId }?.member
    val isOwner = myMembership?.role == "owner"
    val isAdmin = myMembership?.role == "admin" || isOwner
    val groupTitle = group?.title?.takeIf { it.isNotBlank() } ?: "Группа"
    val description = group?.description.orEmpty()

    fun refreshMembers() {
        refreshKey += 1
    }

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 470.dp).heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            color = GroupPanelBackground,
            shadowElevation = 12.dp
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 720.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isBusy, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp)) {
                        Text("‹ Назад", color = GroupPanelAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Информация о группе", color = GroupPanelText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, enabled = !isBusy, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp)) {
                        Text("Готово", color = GroupPanelMuted, fontSize = 11.sp)
                    }
                }

                if (isLoading && group == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GroupPanelAccent)
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color.White)
                                .border(1.dp, GroupPanelBorder, RoundedCornerShape(23.dp))
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GroupPanelAvatar(groupTitle, Modifier.size(78.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                groupTitle,
                                color = GroupPanelText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${members.size} участников",
                                color = GroupPanelMuted,
                                fontSize = 11.sp
                            )
                            if (description.isNotBlank()) {
                                Spacer(Modifier.height(11.dp))
                                Text(
                                    description,
                                    color = GroupPanelMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (isAdmin) {
                                Spacer(Modifier.height(15.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GroupPanelAction(
                                        label = "Изменить",
                                        modifier = Modifier.weight(1f),
                                        onClick = { showEditDialog = true }
                                    )
                                    GroupPanelAction(
                                        label = "Добавить людей",
                                        modifier = Modifier.weight(1f),
                                        onClick = { showInviteDialog = true }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(17.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "УЧАСТНИКИ",
                                color = GroupPanelAccentDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                members.size.toString(),
                                color = GroupPanelMuted,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        if (members.isEmpty() && !isLoading) {
                            Text("Список участников пока пуст.", color = GroupPanelMuted, fontSize = 11.sp)
                        } else {
                            members.forEach { entry ->
                                GroupMemberRow(
                                    entry = entry,
                                    isCurrentUser = entry.member.userId == userId,
                                    showActions = isAdmin && entry.member.userId != userId &&
                                        entry.member.role != "owner" &&
                                        (isOwner || entry.member.role == "member"),
                                    isOwner = isOwner,
                                    isBusy = isBusy,
                                    onPromote = {
                                        scope.launch {
                                            isBusy = true
                                            errorMessage = null
                                            runCatching {
                                                repository.setGroupMemberRole(chatId, entry.member.userId, "admin")
                                            }.onSuccess {
                                                infoMessage = "Участник назначен администратором."
                                                refreshMembers()
                                            }.onFailure {
                                                errorMessage = it.message ?: "Не удалось изменить роль."
                                            }
                                            isBusy = false
                                        }
                                    },
                                    onDemote = {
                                        scope.launch {
                                            isBusy = true
                                            errorMessage = null
                                            runCatching {
                                                repository.setGroupMemberRole(chatId, entry.member.userId, "member")
                                            }.onSuccess {
                                                infoMessage = "Права администратора сняты."
                                                refreshMembers()
                                            }.onFailure {
                                                errorMessage = it.message ?: "Не удалось изменить роль."
                                            }
                                            isBusy = false
                                        }
                                    },
                                    onRemove = { pendingRemoval = entry }
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(errorMessage.orEmpty(), color = GroupPanelDanger, fontSize = 11.sp)
                        }
                        if (infoMessage != null) {
                            Spacer(Modifier.height(5.dp))
                            Text(infoMessage.orEmpty(), color = GroupPanelAccentDark, fontSize = 11.sp)
                        }

                        Spacer(Modifier.height(15.dp))
                        if (isOwner) {
                            GroupPanelDangerAction(
                                label = "Удалить группу",
                                description = "Группа и её переписка будут удалены у всех участников",
                                onClick = { showDeleteConfirmation = true },
                                enabled = !isBusy
                            )
                        } else if (myMembership != null) {
                            GroupPanelDangerAction(
                                label = "Покинуть группу",
                                description = "Группа останется у остальных участников",
                                onClick = { showLeaveConfirmation = true },
                                enabled = !isBusy
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }
    }

    if (showEditDialog && group != null) {
        EditGroupDialog(
            group = group!!,
            isBusy = isBusy,
            onDismiss = { showEditDialog = false },
            onSave = { title, newDescription ->
                scope.launch {
                    isBusy = true
                    errorMessage = null
                    runCatching {
                        repository.updateGroup(chatId, title, newDescription)
                    }.onSuccess {
                        group = group?.copy(title = title.trim(), description = newDescription.trim())
                        onGroupUpdated(title.trim())
                        infoMessage = "Изменения сохранены."
                        showEditDialog = false
                    }.onFailure {
                        errorMessage = it.message ?: "Не удалось изменить группу."
                    }
                    isBusy = false
                }
            }
        )
    }

    if (showInviteDialog) {
        InviteGroupMembersDialog(
            repository = repository,
            userId = userId,
            chatId = chatId,
            currentMemberIds = members.mapTo(mutableSetOf()) { it.member.userId },
            isBusy = isBusy,
            onDismiss = { showInviteDialog = false },
            onInvite = { invitedCount ->
                showInviteDialog = false
                infoMessage = if (invitedCount == 1) "Добавлен 1 новый участник." else "Добавлено участников: $invitedCount."
                refreshMembers()
            },
            onError = { errorMessage = it }
        )
    }

    if (pendingRemoval != null) {
        val target = pendingRemoval!!
        AlertDialog(
            onDismissRequest = { if (!isBusy) pendingRemoval = null },
            containerColor = Color.White,
            title = { Text("Удалить участника?", color = GroupPanelText, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Пользователь ${target.profile?.displayName?.ifBlank { target.profile.username } ?: "Участник"} будет удалён из группы.",
                    color = GroupPanelMuted,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            errorMessage = null
                            runCatching {
                                repository.removeGroupMember(chatId, target.member.userId)
                            }.onSuccess {
                                pendingRemoval = null
                                infoMessage = "Участник удалён из группы."
                                refreshMembers()
                            }.onFailure {
                                errorMessage = it.message ?: "Не удалось удалить участника."
                            }
                            isBusy = false
                        }
                    }
                ) { Text(if (isBusy) "Удаляем…" else "Удалить", color = GroupPanelDanger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }, enabled = !isBusy) {
                    Text("Отмена", color = GroupPanelMuted)
                }
            }
        )
    }

    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showLeaveConfirmation = false },
            containerColor = Color.White,
            title = { Text("Покинуть группу?", color = GroupPanelText, fontWeight = FontWeight.Bold) },
            text = { Text("Ты больше не будешь получать новые сообщения этой группы.", color = GroupPanelMuted, fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            runCatching { repository.leaveGroup(chatId) }
                                .onSuccess { onGroupClosed() }
                                .onFailure {
                                    errorMessage = it.message ?: "Не удалось покинуть группу."
                                    showLeaveConfirmation = false
                                }
                            isBusy = false
                        }
                    }
                ) { Text(if (isBusy) "Выходим…" else "Покинуть", color = GroupPanelDanger) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmation = false }, enabled = !isBusy) {
                    Text("Отмена", color = GroupPanelMuted)
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showDeleteConfirmation = false },
            containerColor = Color.White,
            title = { Text("Удалить группу?", color = GroupPanelText, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Группа «$groupTitle», все сообщения и вложения в её переписке будут недоступны участникам. Это действие нельзя отменить.",
                    color = GroupPanelMuted,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            runCatching { repository.deleteGroup(chatId) }
                                .onSuccess { onGroupClosed() }
                                .onFailure {
                                    errorMessage = it.message ?: "Не удалось удалить группу."
                                    showDeleteConfirmation = false
                                }
                            isBusy = false
                        }
                    }
                ) { Text(if (isBusy) "Удаляем…" else "Удалить навсегда", color = GroupPanelDanger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }, enabled = !isBusy) {
                    Text("Отмена", color = GroupPanelMuted)
                }
            }
        )
    }
}

@Composable
private fun GroupPanelAvatar(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.clip(CircleShape).background(GroupPanelSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title.trim().take(1).uppercase().ifBlank { "G" },
            color = GroupPanelAccent,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GroupPanelAction(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier.clip(RoundedCornerShape(13.dp))
            .background(GroupPanelSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = GroupPanelAccentDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GroupMemberRow(
    entry: FaceVideoGroupMemberProfile,
    isCurrentUser: Boolean,
    showActions: Boolean,
    isOwner: Boolean,
    isBusy: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember(entry.member.userId) { mutableStateOf(false) }
    val profile = entry.profile
    val displayName = profile?.displayName?.ifBlank { profile.username } ?: "Пользователь"
    val subtitle = profile?.let { "@${it.username}" } ?: entry.member.userId.take(8)
    val initials = displayName.trim().take(1).uppercase().ifBlank { "?" }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, GroupPanelBorder.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(GroupPanelSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = GroupPanelAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                displayName + if (isCurrentUser) " · вы" else "",
                color = GroupPanelText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, color = GroupPanelMuted, fontSize = 9.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when (entry.member.role) {
                    "owner" -> "Владелец"
                    "admin" -> "Администратор"
                    else -> "Участник"
                },
                color = if (entry.member.role == "owner" || entry.member.role == "admin") GroupPanelAccentDark else GroupPanelMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (showActions) {
                Box {
                    Text(
                        "⋯",
                        modifier = Modifier.clickable(enabled = !isBusy) { menuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = GroupPanelAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (isOwner) {
                            if (entry.member.role == "admin") {
                                DropdownMenuItem(
                                    text = { Text("Снять права администратора") },
                                    onClick = {
                                        menuExpanded = false
                                        onDemote()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Назначить администратором") },
                                    onClick = {
                                        menuExpanded = false
                                        onPromote()
                                    }
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text("Удалить из группы", color = GroupPanelDanger) },
                            onClick = {
                                menuExpanded = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPanelDangerAction(
    label: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(Color(0xFFFFF5F6))
            .border(1.dp, Color(0xFFF4DDE1), RoundedCornerShape(17.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, color = GroupPanelDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(description, color = GroupPanelMuted, fontSize = 9.sp)
    }
}

@Composable
private fun EditGroupDialog(
    group: FaceVideoGroup,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(group.id) { mutableStateOf(group.title.orEmpty()) }
    var description by remember(group.id) { mutableStateOf(group.description) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = Color.White,
        title = { Text("Редактировать группу", color = GroupPanelText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Название и описание будут видны всем участникам.", color = GroupPanelMuted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название группы") }
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && title.trim().length >= 2,
                onClick = { onSave(title, description) }
            ) {
                Text(if (isBusy) "Сохраняем…" else "Сохранить", color = GroupPanelAccent)
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) {
                Text("Отмена", color = GroupPanelMuted)
            }
        }
    )
}

@Composable
private fun InviteGroupMembersDialog(
    repository: SupabaseChatRepository,
    userId: String,
    chatId: String,
    currentMemberIds: Set<String>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onInvite: (Int) -> Unit,
    onError: (String) -> Unit
) {
    var friends by remember(chatId, userId) { mutableStateOf<List<FaceVideoProfile>>(emptyList()) }
    var selectedIds by remember(chatId, userId) { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember(chatId, userId) { mutableStateOf(true) }
    var localBusy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId, currentMemberIds) {
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
        }.onSuccess {
            friends = it
        }.onFailure {
            loadError = it.message ?: "Не удалось загрузить друзей."
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy && !localBusy) onDismiss() },
        containerColor = Color.White,
        title = { Text("Добавить участников", color = GroupPanelText, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Выбери друзей, которых нужно пригласить в группу.", color = GroupPanelMuted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                when {
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = GroupPanelAccent
                    )
                    loadError != null -> Text(loadError.orEmpty(), color = GroupPanelDanger, fontSize = 11.sp)
                    friends.isEmpty() -> Text(
                        "Все твои друзья уже в группе. Чтобы пригласить нового человека, сначала добавь его в друзья.",
                        color = GroupPanelMuted,
                        fontSize = 11.sp
                    )
                    else -> {
                        Text(
                            "ДРУЗЬЯ · ${selectedIds.size} ВЫБРАНО",
                            color = GroupPanelAccentDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        friends.forEach { profile ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    selectedIds = if (profile.id in selectedIds) selectedIds - profile.id else selectedIds + profile.id
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = profile.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + profile.id else selectedIds - profile.id
                                    }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(profile.displayName.ifBlank { profile.username }, color = GroupPanelText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("@${profile.username}", color = GroupPanelMuted, fontSize = 10.sp)
                                }
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
                        runCatching { repository.addGroupMembers(chatId, selectedIds.toList()) }
                            .onSuccess(onInvite)
                            .onFailure { onError(it.message ?: "Не удалось пригласить участников.") }
                        localBusy = false
                    }
                }
            ) {
                Text(if (localBusy) "Добавляем…" else "Добавить", color = GroupPanelAccent)
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy && !localBusy, onClick = onDismiss) {
                Text("Отмена", color = GroupPanelMuted)
            }
        }
    )
}
