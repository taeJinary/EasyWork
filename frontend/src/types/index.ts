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

export interface AuthUser {
  userId: number;
  email: string;
  nickname: string;
  profileImg: string | null;
  role: string;
}

export type User = AuthUser;

export interface UserProfile {
  userId: number;
  email: string;
  nickname: string;
  profileImg: string | null;
  provider: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  user: AuthUser;
}

export interface SignupRequest {
  nickname: string;
  email: string;
  password: string;
}

export interface SignupResponse {
  userId: number;
  email: string;
  nickname: string;
}

export interface ReissueResponse {
  accessToken: string;
  expiresIn: number;
}

export interface WorkspaceSummary {
  id: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  updatedAt: string;
}

export interface WorkspaceSummaryResponse {
  workspaceId: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
}

export interface WorkspaceListItemResponse {
  workspaceId: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  updatedAt: string;
}

export interface WorkspaceListResponse {
  content: WorkspaceListItemResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface WorkspaceDetailResponse {
  workspaceId: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  updatedAt: string;
}

export interface WorkspaceMemberResponse {
  memberId: number;
  userId: number;
  email: string;
  nickname: string;
  role: 'OWNER' | 'MEMBER';
  joinedAt: string;
}

export interface WorkspaceDetail {
  id: number;
  name: string;
  description?: string;
  memberCount: number;
  myRole: 'OWNER' | 'MEMBER';
  updatedAt: string;
}

export interface WorkspaceMember {
  memberId: number;
  userId: number;
  nickname: string;
  email: string;
  role: 'OWNER' | 'MEMBER';
  joinedAt: string;
}

export interface ProjectSummary {
  id: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  openTaskCount: number;
  taskCount: number;
  doneTaskCount: number;
  progressRate: number;
  updatedAt: string;
}

export interface ProjectListResponse {
  content: ProjectListItemResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ProjectListItemResponse {
  projectId: number;
  name: string;
  description?: string;
  role: 'OWNER' | 'MEMBER';
  memberCount: number;
  taskCount: number;
  doneTaskCount: number;
  progressRate: number;
  updatedAt: string;
}

export interface ProjectTaskSummary {
  todo: number;
  inProgress: number;
  done: number;
}

export type ProjectRole = 'OWNER' | 'MEMBER';

export interface ProjectMember {
  memberId: number;
  userId: number;
  email: string;
  nickname: string;
  role: ProjectRole;
  joinedAt: string;
}

export interface ProjectDetailResponse {
  projectId: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  pendingInvitationCount: number;
  taskSummary: ProjectTaskSummary;
  members: ProjectMember[];
}

export interface ProjectDetail {
  id: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
  pendingInvitationCount: number;
  taskSummary: ProjectTaskSummary;
  members: ProjectMember[];
}

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

export interface Label {
  id: number;
  name: string;
  color: string;
}

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
  memberId: number | null;
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
