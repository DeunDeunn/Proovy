package com.deundeun.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.auth.mapper.UserVerificationMapper;
import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.mapper.ChallengeMapper;
import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.domain.ChatRoomType;
import com.deundeun.chat.dto.ChatRoomListItem;
import com.deundeun.chat.dto.response.ChallengeChatRoomResponse;
import com.deundeun.chat.dto.response.ChatRoomListResponse;
import com.deundeun.chat.dto.response.ChatRoomSummaryResponse;
import com.deundeun.chat.dto.response.DirectChatRoomResponse;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;
import com.deundeun.chat.service.support.ChatUnreadCounter;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@DisplayName("ChatRoomService")
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChallengeMapper challengeMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserVerificationMapper userVerificationMapper;

    @Mock
    private ChatUnreadCounter chatUnreadCounter;

    @Mock
    private ChatRoomMemberFinder chatRoomMemberFinder;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("챌린지 채팅방을 생성한다")
    void createChallengeRoom_success() {
        Long challengeId = 100L;

        ChatRoom room = chatRoomService.createChallengeRoom(challengeId);

        assertThat(room.getChallengeId()).isEqualTo(challengeId);
        assertThat(room.getType()).isEqualTo(ChatRoomType.CHALLENGE);
        verify(chatRoomMapper).insert(room);
    }

    @Test
    @DisplayName("이미 있는 1:1 채팅방이면 그대로 조회한다")
    void createOrGetDirectRoom_existingRoom_reusesRoom() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        ChatRoom existingRoom = ChatRoom.createDirectRoom(ChatRoom.buildDirectChatKey(userId1, userId2));
        ReflectionTestUtils.setField(existingRoom, "id", 10L);
        User partnerUser = User.builder().id(userId2).nickname("지훈").profileImageUrl("url").build();
        ChatRoomMember member = ChatRoomMember.join(existingRoom.getId(), userId1);
        when(chatRoomMapper.findByDirectChatKey("1:2")).thenReturn(Optional.of(existingRoom));
        when(userMapper.findById(userId2)).thenReturn(partnerUser);
        when(chatRoomMemberFinder.findMember(existingRoom.getId(), userId1)).thenReturn(member);

        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId1, userId2);

        assertThat(response.chatRoomId()).isEqualTo(10L);
        assertThat(response.created()).isFalse();
        assertThat(response.partner().nickname()).isEqualTo("지훈");
        assertThat(response.lastMessage()).isNull();
        assertThat(response.unreadCount()).isZero();
        verify(chatRoomMapper, never()).insert(any());
        verify(chatRoomMemberMapper, never()).insert(any());
    }

    @Test
    @DisplayName("없으면 1:1 채팅방을 생성하고 양쪽을 멤버로 등록한다")
    void createOrGetDirectRoom_noExistingRoom_createsRoomAndRegistersBothMembers() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        User partnerUser = User.builder().id(userId2).nickname("지훈").profileImageUrl("url").build();
        when(chatRoomMapper.findByDirectChatKey("1:2")).thenReturn(Optional.empty());
        when(userMapper.findById(userId2)).thenReturn(partnerUser);
        doAnswer(invocation -> {
            ChatRoom saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return null;
        }).when(chatRoomMapper).insert(any());

        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId1, userId2);

        assertThat(response.created()).isTrue();
        assertThat(response.directChatKey()).isEqualTo("1:2");
        assertThat(response.partner().nickname()).isEqualTo("지훈");
        assertThat(response.lastMessage()).isNull();
        assertThat(response.unreadCount()).isZero();
        assertThat(response.lastReadMessageId()).isNull();
        assertThat(response.lastReadAt()).isNull();

        ArgumentCaptor<ChatRoomMember> captor = ArgumentCaptor.forClass(ChatRoomMember.class);
        verify(chatRoomMemberMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(ChatRoomMember::getUserId)
            .containsExactlyInAnyOrder(userId1, userId2);
    }

    @Test
    @DisplayName("자기 자신과의 1:1 채팅방 생성은 예외를 던진다")
    void createOrGetDirectRoom_selfChat_throwsException() {
        Long userId = 1L;

        assertThatThrownBy(() -> chatRoomService.createOrGetDirectRoom(userId, userId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_SELF_CHAT_NOT_ALLOWED);

        verify(chatRoomMapper, never()).findByDirectChatKey(any());
        verify(chatRoomMapper, never()).insert(any());
    }

    @Test
    @DisplayName("동시 생성으로 인한 중복 삽입 시(ON CONFLICT DO NOTHING) 기존 방을 재조회한다")
    void createOrGetDirectRoom_concurrentInsertConflict_fallsBackToExistingRoom() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        ChatRoom existingRoom = ChatRoom.createDirectRoom("1:2");
        ReflectionTestUtils.setField(existingRoom, "id", 20L);
        User partnerUser = User.builder().id(userId2).nickname("지훈").profileImageUrl("url").build();
        ChatRoomMember member = ChatRoomMember.join(existingRoom.getId(), userId1);

        when(chatRoomMapper.findByDirectChatKey("1:2"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existingRoom));
        // insert가 스텁 없이 호출되면 room.getId()는 null로 남음 — ON CONFLICT DO NOTHING으로 실제 삽입이 스킵된 상황을 재현
        when(userMapper.findById(userId2)).thenReturn(partnerUser);
        when(chatRoomMemberFinder.findMember(existingRoom.getId(), userId1)).thenReturn(member);

        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId1, userId2);

        assertThat(response.chatRoomId()).isEqualTo(20L);
        assertThat(response.created()).isFalse();
        verify(chatRoomMemberMapper, never()).insert(any());
    }

    @Test
    @DisplayName("챌린지 채팅방이 없으면 예외를 던진다")
    void getChallengeRoom_roomNotFound_throwsException() {
        Long challengeId = 100L;
        Long userId = 1L;
        when(challengeMapper.findById(challengeId)).thenReturn(Challenge.builder().id(challengeId).title("매일 아침 7시 기상").build());
        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.getChallengeRoom(challengeId, userId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);

        verify(chatRoomMemberFinder, never()).findMember(any(), any());
    }

    @Test
    @DisplayName("챌린지 채팅방 id를 멤버십 검증 없이 조회한다")
    void getChatRoomIdByChallengeId_returnsChatRoomId() {
        Long challengeId = 100L;
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);
        ReflectionTestUtils.setField(room, "id", 5L);
        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.of(room));

        Long chatRoomId = chatRoomService.getChatRoomIdByChallengeId(challengeId);

        assertThat(chatRoomId).isEqualTo(5L);
        verify(chatRoomMemberFinder, never()).findMember(any(), any());
    }

    @Test
    @DisplayName("조회 대상 챌린지 채팅방이 없으면 예외를 던진다")
    void getChatRoomIdByChallengeId_roomNotFound_throwsException() {
        Long challengeId = 100L;
        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.getChatRoomIdByChallengeId(challengeId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("참여자가 아니면 접근 검증 예외가 그대로 전파된다")
    void getChallengeRoom_notMember_propagatesValidatorException() {
        Long challengeId = 100L;
        Long userId = 1L;
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);
        ReflectionTestUtils.setField(room, "id", 5L);
        when(challengeMapper.findById(challengeId)).thenReturn(Challenge.builder().id(challengeId).title("매일 아침 7시 기상").build());
        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.of(room));
        when(chatRoomMemberFinder.findMember(5L, userId))
            .thenThrow(new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN));

        assertThatThrownBy(() -> chatRoomService.getChallengeRoom(challengeId, userId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("챌린지 채팅방 조회 시 멤버수/안읽음수/읽음정보를 전부 반환한다")
    void getChallengeRoom_success_returnsFullData() {
        Long challengeId = 100L;
        Long userId = 1L;
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);
        ReflectionTestUtils.setField(room, "id", 5L);

        ChatRoomMember member = ChatRoomMember.join(5L, userId);
        ReflectionTestUtils.setField(member, "lastReadMessageId", 20L);

        Long senderId = 4L;
        ChatMessage lastMessage = ChatMessage.create(5L, senderId, "오늘 인증 완료했습니다!", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(lastMessage, "id", 25L);
        User sender = User.builder().id(senderId).nickname("민기").build();

        when(challengeMapper.findById(challengeId)).thenReturn(Challenge.builder().id(challengeId).title("매일 아침 7시 기상").build());
        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.of(room));
        when(chatRoomMemberFinder.findMember(5L, userId)).thenReturn(member);
        when(chatRoomMemberMapper.findActiveByChatRoomId(5L))
            .thenReturn(List.of(ChatRoomMember.join(5L, userId), ChatRoomMember.join(5L, 2L)));
        when(chatMessageMapper.findLatestByChatRoomId(5L, 1)).thenReturn(List.of(lastMessage));
        when(userMapper.findById(senderId)).thenReturn(sender);
        when(chatUnreadCounter.count(member)).thenReturn(3);

        ChallengeChatRoomResponse response = chatRoomService.getChallengeRoom(challengeId, userId);

        assertThat(response.chatRoomId()).isEqualTo(5L);
        assertThat(response.challengeTitle()).isEqualTo("매일 아침 7시 기상");
        assertThat(response.memberCount()).isEqualTo(2);
        assertThat(response.lastMessage().content()).isEqualTo("오늘 인증 완료했습니다!");
        assertThat(response.lastMessage().senderNickname()).isEqualTo("민기");
        assertThat(response.unreadCount()).isEqualTo(3);
        assertThat(response.lastReadMessageId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("방이 없으면 배치 조회 없이 빈 목록을 반환한다")
    void getMyRooms_noRooms_returnsEmptyContentWithoutBatchLookups() {
        Long userId = 1L;
        when(chatRoomMemberMapper.findRoomsByUserId(userId, 0, 20)).thenReturn(List.of());
        when(chatRoomMemberMapper.countRoomsByUserId(userId)).thenReturn(0);

        ChatRoomListResponse response = chatRoomService.getMyRooms(userId, 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(challengeMapper, never()).findByIds(any());
        verify(userMapper, never()).findByIds(any());
        verify(chatUnreadCounter, never()).countBatch(anyMap());
    }

    @Test
    @DisplayName("챌린지 채팅방 목록 조회 시 챌린지 제목과 마지막 메시지, 안 읽은 개수를 반환한다")
    void getMyRooms_challengeRoom_assemblesChallengeTitleAndUnreadCount() {
        Long userId = 1L;
        ChatRoomListItem item = new ChatRoomListItem(
            10L, ChatRoomType.CHALLENGE, 7L, null,
            ChatRoom.createChallengeRoom(7L).getCreatedAt(),
            20L, null,
            25L, 3L, "오늘 인증 완료했습니다!", ChatMessageType.TEXT, null, null
        );
        Challenge challenge = Challenge.builder().id(7L).title("매일 아침 7시 기상").build();
        User sender = User.builder().id(3L).nickname("민기").build();

        when(chatRoomMemberMapper.findRoomsByUserId(userId, 0, 20)).thenReturn(List.of(item));
        when(chatRoomMemberMapper.countRoomsByUserId(userId)).thenReturn(1);
        when(challengeMapper.findByIds(List.of(7L))).thenReturn(List.of(challenge));
        when(userMapper.findByIds(List.of(3L))).thenReturn(List.of(sender));
        when(chatUnreadCounter.countBatch(Map.of(10L, 20L))).thenReturn(Map.of(10L, 5));

        ChatRoomListResponse response = chatRoomService.getMyRooms(userId, 0, 20);

        ChatRoomSummaryResponse summary = response.content().get(0);
        assertThat(summary.challengeTitle()).isEqualTo("매일 아침 7시 기상");
        assertThat(summary.directChatPartner()).isNull();
        assertThat(summary.lastMessage().senderNickname()).isEqualTo("민기");
        assertThat(summary.unreadCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("1:1 채팅방을 한 번도 읽지 않은 경우 상대방 정보를 조회하고 읽음 기준값을 0으로 처리한다")
    void getMyRooms_directRoomWithNoReadsYet_resolvesPartnerAndNormalizesNullCursorToZero() {
        Long userId = 1L;
        Long partnerId = 2L;
        ChatRoomListItem item = new ChatRoomListItem(
            11L, ChatRoomType.DIRECT, null, "1:2",
            ChatRoom.createDirectRoom("1:2").getCreatedAt(),
            null, null,
            null, null, null, null, null, null
        );
        User partner = User.builder().id(partnerId).nickname("지훈").profileImageUrl("url").build();

        when(chatRoomMemberMapper.findRoomsByUserId(userId, 0, 20)).thenReturn(List.of(item));
        when(chatRoomMemberMapper.countRoomsByUserId(userId)).thenReturn(1);
        when(userMapper.findByIds(List.of(partnerId))).thenReturn(List.of(partner));
        when(userVerificationMapper.findApprovedUserIds(List.of(partnerId))).thenReturn(List.of(partnerId));
        when(chatUnreadCounter.countBatch(any())).thenReturn(Map.of());

        ChatRoomListResponse response = chatRoomService.getMyRooms(userId, 0, 20);

        ChatRoomSummaryResponse summary = response.content().get(0);
        assertThat(summary.directChatPartner().nickname()).isEqualTo("지훈");
        assertThat(summary.directChatPartner().badgeApproved()).isTrue();
        assertThat(summary.lastMessage()).isNull();
        assertThat(summary.unreadCount()).isZero();
        verify(challengeMapper, never()).findByIds(any());

        ArgumentCaptor<Map<Long, Long>> captor = ArgumentCaptor.forClass(Map.class);
        verify(chatUnreadCounter).countBatch(captor.capture());
        assertThat(captor.getValue()).containsEntry(11L, 0L);
    }
}
