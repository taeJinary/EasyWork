// ── Common ──
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ── User ──
export interface User {
  id: number;
  email: string;
  name: string;
  profileImageUrl?: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  accessTokenExpiresIn: number;
  user: User;
}

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
}

export interface SignupResponse {
  id: number;
  email: string;
  name: string;
}

// ── Workspace ──
export interface WorkspaceSummary {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceListResponse {
  workspaces: WorkspaceSummary[];
  pageInfo: PageInfo;
}

export interface WorkspaceDetail {
  id: number;
  name: string;
  description?: string;
  memberCount: number;
  projectCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceMember {
  id: number;
  userId: number;
  name: string;
  email: string;
  role: 'OWNER' | 'MEMBER';
  joinedAt: string;
}

// ── Project ──
export interface ProjectSummary {
  id: number;
  name: string;
  description?: string;
  workspaceId: number;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  openTaskCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectListResponse {
  projects: ProjectSummary[];
  pageInfo: PageInfo;
}

export interface ProjectDetail {
  id: number;
  name: string;
  description?: string;
  workspaceId: number;
  workspaceName: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  openTaskCount: number;
  createdAt: string;
  updatedAt: string;
}

// ProjectMember is defined in the Phase C section below (matches backend ProjectMemberResponse)

// ── Task ──
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface TaskSummary {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignee?: User;
  labels: Label[];
  dueDate?: string;
  commentCount: number;
  attachmentCount: number;
  createdAt: string;
  updatedAt: string;
}

// ── Board Response (matches backend TaskBoardResponse) ──
export interface BoardAssignee {
  userId: number;
  nickname: string;
}

export interface BoardLabel {
  labelId: number;
  name: string;
  colorHex: string;
}

export interface BoardTaskCard {
  taskId: number;
  title: string;
  priority: TaskPriority;
  dueDate?: string;
  position: number;
  version: number;
  assignee?: BoardAssignee;
  labels: BoardLabel[];
  commentCount: number;
}

export interface BoardColumn {
  status: TaskStatus;
  tasks: BoardTaskCard[];
}

export interface BoardFilterResponse {
  assigneeUserId?: number;
  priority?: TaskPriority;
  labelId?: number;
  keyword?: string;
}

export interface TaskBoardResponse {
  projectId: number;
  filters: BoardFilterResponse;
  columns: BoardColumn[];
}

// ── List Response (matches backend TaskListResponse + TaskListItemResponse) ──
export interface TaskListItemAssignee {
  userId: number;
  nickname: string;
}

export interface TaskListItem {
  taskId: number;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string;
  position: number;
  version: number;
  commentCount: number;
  assignee?: TaskListItemAssignee;
}

export interface TaskListResponse {
  content: TaskListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// ── Task Detail (matches backend TaskDetailResponse) ──
export interface TaskDetailUserSummary {
  userId: number;
  nickname: string;
}

export interface TaskDetailLabel {
  labelId: number;
  name: string;
  colorHex: string;
}

export interface TaskStatusHistory {
  historyId: number;
  fromStatus: TaskStatus;
  toStatus: TaskStatus;
  changedBy: TaskDetailUserSummary;
  changedAt: string;
}

export interface TaskDetail {
  taskId: number;
  projectId: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string;
  position: number;
  version: number;
  creator: TaskDetailUserSummary;
  assignee?: TaskDetailUserSummary;
  labels: TaskDetailLabel[];
  commentCount: number;
  recentStatusHistories: TaskStatusHistory[];
  createdAt: string;
  updatedAt: string;
}

// ── Comment (matches backend CommentResponse / CommentListResponse) ──
export interface CommentAuthor {
  userId: number;
  nickname: string;
}

export interface Comment {
  commentId: number;
  content: string;
  author: CommentAuthor;
  createdAt: string;
  updatedAt: string;
  editable: boolean;
}

export interface CommentListResponse {
  content: Comment[];
  nextCursor?: number;
  hasNext: boolean;
}

// ── Label ──
export interface Label {
  id: number;
  name: string;
  color: string;
}

// ── Project Member (matches backend ProjectMemberResponse) ──
export type ProjectRole = 'OWNER' | 'MEMBER';

export interface ProjectMember {
  memberId: number;
  userId: number;
  email: string;
  nickname: string;
  role: ProjectRole;
  joinedAt: string;
}

// ── Invitation (matches backend InvitationListItemResponse / InvitationListResponse) ──
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED' | 'EXPIRED';

export interface InvitationListItem {
  invitationId: number;
  projectId: number;
  projectName: string;
  inviterUserId: number;
  inviterNickname: string;
  role: ProjectRole;
  status: InvitationStatus;
  expiresAt: string;
  createdAt: string;
}

export interface InvitationListResponse {
  content: InvitationListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface InvitationAction {
  invitationId: number;
  projectId: number;
  memberId: number;
  role: ProjectRole;
  status: InvitationStatus;
}

export interface InvitationSummary {
  invitationId: number;
  projectId: number;
  inviteeUserId: number;
  inviteeEmail: string;
  inviteeNickname: string;
  role: ProjectRole;
  status: InvitationStatus;
  expiresAt: string;
}

// ── Notification (matches backend NotificationListItemResponse / NotificationListResponse) ──
export type NotificationType =
  | 'PROJECT_INVITED'
  | 'INVITATION_ACCEPTED'
  | 'TASK_ASSIGNED'
  | 'COMMENT_CREATED'
  | 'COMMENT_MENTIONED';

export type NotificationReferenceType = 'PROJECT' | 'TASK' | 'INVITATION' | 'COMMENT';

export interface NotificationItem {
  notificationId: number;
  type: NotificationType;
  title: string;
  content: string;
  referenceType: NotificationReferenceType;
  referenceId: number;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationListResponse {
  content: NotificationItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface NotificationUnreadCount {
  unreadCount: number;
}

// ── Attachment (matches backend TaskAttachmentResponse) ──
export interface Attachment {
  attachmentId: number;
  taskId: number;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploaderUserId: number;
  uploaderNickname: string;
  createdAt: string;
}
