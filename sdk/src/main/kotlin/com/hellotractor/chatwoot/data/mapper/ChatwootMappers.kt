package com.hellotractor.chatwoot.data.mapper

import com.hellotractor.chatwoot.data.local.entity.ChatwootContactEntity
import com.hellotractor.chatwoot.data.local.entity.ChatwootConversationEntity
import com.hellotractor.chatwoot.data.local.entity.ChatwootMessageEntity
import com.hellotractor.chatwoot.data.remote.dto.*
import com.hellotractor.chatwoot.data.remote.request.CreateContactRequest
import com.hellotractor.chatwoot.data.remote.request.UpdateContactRequest
import com.hellotractor.chatwoot.domain.model.*

fun ChatwootContactDto.toDomain(): ChatwootContact = ChatwootContact(
    id = id ?: 0,
    contactIdentifier = sourceId ?: identifier,
    pubsubToken = pubsubToken,
    name = name,
    email = email
)

fun ChatwootMessageDto.toDomain(): ChatwootMessage = ChatwootMessage(
    id = id ?: 0,
    content = content,
    messageType = ChatwootMessageType.fromValue(messageType ?: 0),
    contentType = contentType,
    createdAt = createdAt ?: 0L,
    conversationId = conversationId ?: 0,
    attachments = attachments?.map { it.toDomain() } ?: emptyList(),
    sender = sender?.toDomain(),
    echoId = echoId
)

fun ChatwootAttachmentDto.toDomain(): ChatwootAttachment = ChatwootAttachment(
    id = id,
    fileType = fileType,
    dataUrl = dataUrl,
    thumbUrl = thumbUrl,
    fileSize = fileSize
)

fun ChatwootSenderDto.toDomain(): ChatwootMessageSender = ChatwootMessageSender(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    type = type
)

fun ChatwootConversationDto.toDomain(): ChatwootConversation = ChatwootConversation(
    id = id ?: 0,
    inboxId = inboxId,
    messages = messages?.map { it.toDomain() } ?: emptyList(),
    contact = contact?.toDomain(),
    status = status
)

fun ChatwootContact.toEntity(): ChatwootContactEntity = ChatwootContactEntity(
    id = id,
    contactIdentifier = contactIdentifier,
    pubsubToken = pubsubToken,
    name = name,
    email = email
)

fun ChatwootMessage.toEntity(): ChatwootMessageEntity = ChatwootMessageEntity(
    id = id,
    content = content,
    messageType = messageType.value,
    contentType = contentType,
    createdAt = createdAt,
    conversationId = conversationId,
    attachments = attachments,
    senderName = sender?.name,
    senderAvatarUrl = sender?.avatarUrl,
    senderType = sender?.type,
    echoId = echoId
)

fun ChatwootConversation.toEntity(): ChatwootConversationEntity = ChatwootConversationEntity(
    id = id,
    inboxId = inboxId,
    contactId = contact?.id,
    status = status
)

fun ChatwootContactEntity.toDomain(): ChatwootContact = ChatwootContact(
    id = id,
    contactIdentifier = contactIdentifier,
    pubsubToken = pubsubToken,
    name = name,
    email = email
)

fun ChatwootMessageEntity.toDomain(): ChatwootMessage = ChatwootMessage(
    id = id,
    content = content,
    messageType = ChatwootMessageType.fromValue(messageType),
    contentType = contentType,
    createdAt = createdAt,
    conversationId = conversationId,
    attachments = attachments,
    sender = if (senderName != null || senderAvatarUrl != null) {
        ChatwootMessageSender(name = senderName, avatarUrl = senderAvatarUrl, type = senderType)
    } else null,
    echoId = echoId
)

fun ChatwootConversationEntity.toDomain(): ChatwootConversation = ChatwootConversation(
    id = id,
    inboxId = inboxId,
    status = status
)

fun ChatwootUser.toCreateRequest(): CreateContactRequest = CreateContactRequest(
    identifier = identifier,
    identifierHash = identifierHash,
    name = name,
    email = email,
    avatarUrl = avatarUrl,
    customAttributes = customAttributes
)

fun ChatwootUser.toUpdateRequest(): UpdateContactRequest = UpdateContactRequest(
    identifier = identifier,
    identifierHash = identifierHash,
    name = name,
    email = email,
    avatarUrl = avatarUrl,
    customAttributes = customAttributes
)
