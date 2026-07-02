// types/dto.ts — mirrors backend DTOs (com.message.mesh.dto)
export type ConversationType = 'DIRECT' | 'GROUP';
export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ';
export type MembershipRole = 'ADMIN' | 'MEMBER';
export type UserRole = 'USER' | 'ADMIN';

export interface UserDto {
  id: string;
  username: string;
  displayName: string;
  role: UserRole;
}

// Admin-only projection (com.message.mesh.dto.AdminUserDto)
export interface AdminUserDto {
  id: string;
  username: string;
  displayName: string;
  role: UserRole;
  active: boolean;
  createdAt: string; // ISO
  online: boolean;
  conversationCount: number;
}

// Generic pagination envelope (com.message.mesh.dto.PagedResponse)
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MessageReactionSummary {
  emoji: string;
  count: number;
  usernames: string[];
}

export interface MessageParentPreview {
  id: string;
  senderUsername: string;
  body: string;
}

export interface MessageDto {
  id: string;
  conversationId: string;
  senderUsername: string;
  seq: number;
  body: string;
  status: MessageStatus;
  createdAt: string; // ISO
  clientTempId?: string | null;
  editedAt?: string | null;
  deleted: boolean;
  parentId?: string | null;
  parentPreview?: MessageParentPreview | null;
  reactions: MessageReactionSummary[];
}

export interface ConversationDto {
  id: string;
  type: ConversationType;
  title: string;
  lastMessage?: MessageDto | null;
  unreadCount: number;
  memberCount: number;
  muted: boolean;
  archived: boolean;
}

export interface ConversationMemberDto {
  userId: string;
  username: string;
  displayName: string;
  role: MembershipRole;
}

// Admin-only projection (com.message.mesh.dto.AdminConversationDto)
export interface AdminConversationDto {
  id: string;
  type: ConversationType;
  title: string;
  memberCount: number;
  messageCount: number;
  createdAt: string; // ISO
  deleted: boolean;
}

// Admin user detail with the conversations they belong to (com.message.mesh.dto.AdminUserDetailDto)
export interface AdminUserDetailDto {
  user: AdminUserDto;
  conversations: AdminConversationDto[];
}

// Aggregate dashboard metrics (com.message.mesh.dto.AdminStatsDto)
export interface AdminStatsDto {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  adminUsers: number;
  onlineUsers: number;
  newUsersLast7Days: number;
  totalConversations: number;
  groupConversations: number;
  directConversations: number;
  deletedConversations: number;
  totalMessages: number;
  messagesLast7Days: number;
  auditEvents: number;
}

// Audit trail entry (com.message.mesh.dto.AuditEventDto)
export interface AuditEventDto {
  id: string;
  actorUsername: string;
  action: string;
  targetType?: string | null;
  targetId?: string | null;
  details?: string | null;
  createdAt: string; // ISO
}

// Conversation/membership meta event (com.message.mesh.dto.ConversationEvent)
export type ConversationEventType =
  | 'RENAMED'
  | 'MEMBER_ADDED'
  | 'MEMBER_REMOVED'
  | 'MEMBER_ROLE_CHANGED'
  | 'CONVERSATION_DELETED';

export interface ConversationEvent {
  type: ConversationEventType;
  conversationId: string;
  actorUsername?: string | null;
  targetUsername?: string | null;
  title?: string | null;
}

export interface AuthResponse {
  token: string;
  user: UserDto;
}

// Outgoing payloads
export interface RegisterRequest {
  username: string;
  password: string;
  displayName: string;
}

// Admin request to provision a user (com.message.mesh.dto.CreateUserRequest)
export interface CreateUserRequest {
  username: string;
  password: string;
  displayName: string;
  role: UserRole;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface SendMessageRequest {
  conversationId: string;
  body: string;
  clientTempId: string;
  parentId?: string | null;
}

export interface UpdateProfileRequest {
  displayName: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface RenameConversationRequest {
  title: string;
}

export interface AddMembersRequest {
  usernames: string[];
}

export interface UpdateMemberRoleRequest {
  role: MembershipRole;
}

export interface MembershipPrefsRequest {
  muted?: boolean;
  archived?: boolean;
}

export interface EditMessageRequest {
  body: string;
}

export interface AckRequest {
  messageId: string;
}

export interface TypingEvent {
  conversationId: string;
}

export interface CreateConversationRequest {
  type: ConversationType;
  title?: string;
  memberUsernames: string[];
}

// Server -> client small payloads
export interface AckDto {
  messageId: string;
  status: MessageStatus;
}

export interface PresenceDto {
  username: string;
  online: boolean;
}

export interface TypingNotification {
  conversationId: string;
  username: string;
}

// UI-local message status (adds optimistic SENDING + FAILED)
export type LocalMessageStatus = MessageStatus | 'SENDING' | 'FAILED';

export interface LocalMessage extends Omit<MessageDto, 'status'> {
  status: LocalMessageStatus;
}
