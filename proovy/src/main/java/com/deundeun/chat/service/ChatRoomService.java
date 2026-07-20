package com.deundeun.chat.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.auth.mapper.UserVerificationMapper;
import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.mapper.ChallengeMapper;
import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.domain.ChatRoomType;
import com.deundeun.chat.dto.ChatRoomListItem;
import com.deundeun.chat.dto.response.*;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;
import com.deundeun.chat.service.support.ChatUnreadCounter;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final ChatRoomMemberMapper chatRoomMemberMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChallengeMapper challengeMapper;
    private final UserMapper userMapper;
    private final UserVerificationMapper userVerificationMapper;

    private final ChatRoomMemberFinder chatRoomMemberFinder;
    private final ChatUnreadCounter chatUnreadCounter;

    @Transactional
    public ChatRoom createChallengeRoom(Long challengeId) {
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);

        chatRoomMapper.insert(room);
        log.info("[Chat] 챌린지 채팅방 생성 완료: challengeId={}, chatRoomId={}", challengeId, room.getId());

        return room;
    }

    @Transactional
    public DirectChatRoomResponse createOrGetDirectRoom(Long userId1, Long userId2) {
        validateNotSelfChat(userId1, userId2);

        String directChatKey = ChatRoom.buildDirectChatKey(userId1, userId2);
        DirectChatPartnerResponse partner = buildPartner(userId2);

        return chatRoomMapper.findByDirectChatKey(directChatKey)
            .map(room -> {
                log.debug("[Chat] 기존 1:1 채팅방 조회: chatRoomId={}, directChatKey={}", room.getId(), directChatKey);
                return buildExistingRoomResponse(room, userId1, partner);
            })
            .orElseGet(() -> createDirectRoom(directChatKey, userId1, userId2, partner));
    }

    private DirectChatRoomResponse createDirectRoom(String directChatKey, Long userId1, Long userId2,
                                                      DirectChatPartnerResponse partner) {
        ChatRoom room = ChatRoom.createDirectRoom(directChatKey);
        chatRoomMapper.insert(room);

        if (room.getId() == null) {
            log.debug("[Chat] 동시 요청으로 인한 1:1 채팅방 중복 생성 시도, 기존 방 재조회: directChatKey={}", directChatKey);
            ChatRoom existingRoom = chatRoomMapper.findByDirectChatKey(directChatKey)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            return buildExistingRoomResponse(existingRoom, userId1, partner);
        }

        ChatRoomMember member1 = ChatRoomMember.join(room.getId(), userId1);
        chatRoomMemberMapper.insert(member1);
        chatRoomMemberMapper.insert(ChatRoomMember.join(room.getId(), userId2));
        log.info("[Chat] 1:1 채팅방 생성 완료: chatRoomId={}, directChatKey={}", room.getId(), directChatKey);

        return DirectChatRoomResponse.of(
            room, true, partner,
            null, 0,
            member1.getLastReadMessageId(), member1.getLastReadAt()
        );
    }

    private DirectChatRoomResponse buildExistingRoomResponse(ChatRoom room, Long callerId, DirectChatPartnerResponse partner) {
        ChatRoomMember member = chatRoomMemberFinder.findMember(room.getId(), callerId);
        LastMessageResponse lastMessage = findLastMessage(room.getId());
        int unreadCount = chatUnreadCounter.count(room.getId(), member.getLastReadMessageId());

        return DirectChatRoomResponse.of(
            room, false, partner,
            lastMessage, unreadCount,
            member.getLastReadMessageId(), member.getLastReadAt()
        );
    }

    private DirectChatPartnerResponse buildPartner(Long partnerId) {
        User partner = userMapper.findById(partnerId);
        if (partner == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        boolean approved = !userVerificationMapper.findApprovedUserIds(List.of(partnerId)).isEmpty();

        return DirectChatPartnerResponse.of(partner, approved);
    }

    private LastMessageResponse findLastMessage(Long chatRoomId) {
        return chatMessageMapper.findLatestByChatRoomId(chatRoomId, 1).stream()
            .findFirst()
            .map(message -> LastMessageResponse.of(message, resolveNickname(message.getSenderId())))
            .orElse(null);
    }

    private String resolveNickname(Long userId) {
        User user = userMapper.findById(userId);
        return user != null ? user.getNickname() : null;
    }

    private void validateNotSelfChat(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            log.debug("[Chat] 자기 자신과의 1:1 채팅방 생성 시도: userId={}", userId1);
            throw new ApiException(ErrorCode.CHAT_ROOM_SELF_CHAT_NOT_ALLOWED);
        }
    }

    @Transactional(readOnly = true)
    public ChallengeChatRoomResponse getChallengeRoom(Long challengeId, Long userId) {
        Challenge challenge = findChallenge(challengeId);
        ChatRoom room = getChatRoomByChallengeId(challengeId);
        ChatRoomMember member = chatRoomMemberFinder.findMember(room.getId(), userId);

        int memberCount = chatRoomMemberMapper.findActiveByChatRoomId(room.getId()).size();
        LastMessageResponse lastMessage = findLastMessage(room.getId());
        int unreadCount = chatUnreadCounter.count(member);

        log.debug(
            "[Chat] 챌린지 채팅방 조회 완료: challengeId={}, chatRoomId={}, userId={}, memberCount={}, unreadCount={}",
            challengeId, room.getId(), userId, memberCount, unreadCount
        );

        return ChallengeChatRoomResponse.of(room, challenge.getTitle(), memberCount, lastMessage, unreadCount, member);
    }
    
    @Transactional(readOnly = true)
    public Long getChatRoomIdByChallengeId(Long challengeId) {
        return getChatRoomByChallengeId(challengeId).getId();
    }

    private Challenge findChallenge(Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        return challenge;
    }

    private ChatRoom getChatRoomByChallengeId(Long challengeId) {
        return chatRoomMapper.findByChallengeId(challengeId)
            .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ChatRoomListResponse getMyRooms(Long userId, int page, int size) {
        List<ChatRoomListItem> items = chatRoomMemberMapper.findRoomsByUserId(userId, page * size, size);
        long totalElements = chatRoomMemberMapper.countRoomsByUserId(userId);
        log.debug("[Chat] 내 채팅방 목록 조회 완료: userId={}, page={}, size={}, totalElements={}",
            userId, page, size, totalElements);

        return ChatRoomListResponse.of(assembleSummaries(items, userId), page, size, totalElements);
    }

    private List<ChatRoomSummaryResponse> assembleSummaries(List<ChatRoomListItem> items, Long userId) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, String> challengeTitlesById = findChallengeTitles(items);
        Map<Long, User> usersById = findRelevantUsers(items, userId);
        Set<Long> approvedUserIds = findApprovedUserIds(usersById.keySet());
        Map<Long, Integer> unreadCountsByRoomId = chatUnreadCounter.countBatch(
            items.stream().collect(Collectors.toMap(
                ChatRoomListItem::chatRoomId,
                item -> item.lastReadMessageId() != null ? item.lastReadMessageId() : 0L)));

        return items.stream()
            .map(item -> ChatRoomSummaryResponse.of(
                item,
                challengeTitlesById.get(item.challengeId()),
                findDirectChatPartner(item, userId, usersById, approvedUserIds),
                resolveNickname(usersById, item.lastMessageSenderId()),
                unreadCountsByRoomId.getOrDefault(item.chatRoomId(), 0)
            ))
            .toList();
    }

    private Map<Long, String> findChallengeTitles(List<ChatRoomListItem> items) {
        List<Long> challengeIds = items.stream()
            .map(ChatRoomListItem::challengeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (challengeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return challengeMapper.findByIds(challengeIds).stream()
            .collect(Collectors.toMap(Challenge::getId, Challenge::getTitle));
    }

    private Map<Long, User> findRelevantUsers(List<ChatRoomListItem> items, Long userId) {
        List<Long> userIds = Stream.concat(
                items.stream()
                    .filter(item -> item.chatRoomType() == ChatRoomType.DIRECT)
                    .map(item -> extractPartnerId(item.directChatKey(), userId)),
                items.stream().map(ChatRoomListItem::lastMessageSenderId).filter(Objects::nonNull)
            )
            .distinct()
            .toList();

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userMapper.findByIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Set<Long> findApprovedUserIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptySet();
        }

        return new HashSet<>(userVerificationMapper.findApprovedUserIds(List.copyOf(userIds)));
    }

    private DirectChatPartnerResponse findDirectChatPartner(ChatRoomListItem item, Long userId,
                                                              Map<Long, User> usersById, Set<Long> approvedUserIds) {
        if (item.chatRoomType() != ChatRoomType.DIRECT) {
            return null;
        }

        Long partnerId = extractPartnerId(item.directChatKey(), userId);
        User partner = usersById.get(partnerId);
        return partner != null ? DirectChatPartnerResponse.of(partner, approvedUserIds.contains(partnerId)) : null;
    }

    private String resolveNickname(Map<Long, User> usersById, Long userId) {
        User user = usersById.get(userId);
        return user != null ? user.getNickname() : null;
    }

    private Long extractPartnerId(String directChatKey, Long userId) {
        String[] parts = directChatKey.split(":");
        long first = Long.parseLong(parts[0]);
        long second = Long.parseLong(parts[1]);

        return first == userId ? second : first;
    }
}
