package com.facevideo.app.data

import com.facevideo.app.model.ChatMessage
import com.facevideo.app.model.ChatSummary

object DemoData {
    val chats = listOf(
        ChatSummary("maria", "Мария Иванова", "Тогда до встречи! ✨", "16:42", 2, true, "МИ", 0),
        ChatSummary("alex", "Александр", "Отправил тебе фото", "15:18", 0, true, "А", 1),
        ChatSummary("team", "Наша команда", "Денис: Отличная идея", "14:05", 4, false, "К", 2),
        ChatSummary("anna", "Анна", "Давай созвонимся вечером?", "Вчера", 0, false, "АИ", 3),
        ChatSummary("max", "Максим", "Спасибо, получил", "Вт", 0, false, "М", 1)
    )
    val messages = mapOf(
        "maria" to listOf(
            ChatMessage("m1", "Привет! Как проходит день?", "16:35", false),
            ChatMessage("m2", "Всё отлично, спасибо 😊 А у тебя?", "16:37", true),
            ChatMessage("m3", "Тоже хорошо! Может, увидимся сегодня?", "16:39", false),
            ChatMessage("m4", "Конечно. Тогда до встречи! ✨", "16:42", false)
        ),
        "alex" to listOf(
            ChatMessage("a1", "Посмотри, что получилось", "15:12", false),
            ChatMessage("a2", "Классный кадр!", "15:15", true),
            ChatMessage("a3", "Отправил тебе фото", "15:18", false)
        ),
        "team" to listOf(
            ChatMessage("t1", "Всем привет! Обсудим планы?", "13:50", false),
            ChatMessage("t2", "Я уже подготовил несколько идей", "13:58", true),
            ChatMessage("t3", "Денис: Отличная идея", "14:05", false)
        ),
        "anna" to listOf(ChatMessage("n1", "Давай созвонимся вечером?", "Вчера", false)),
        "max" to listOf(ChatMessage("x1", "Спасибо, получил", "Вт", false))
    )
}
