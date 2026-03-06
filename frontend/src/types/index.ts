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

export interface ProjectMember {
  id: number;
  userId: number;
  name: string;
  email: string;
  role: 'OWNER' | 'MEMBER';
  joinedAt: string;
}

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

export interface TaskBoardResponse {
  todo: TaskSummary[];
  inProgress: TaskSummary[];
  done: TaskSummary[];
}

export interface TaskListResponse {
  tasks: TaskSummary[];
  pageInfo: PageInfo;
}

export interface TaskDetail {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignee?: User;
  creator: User;
  labels: Label[];
  dueDate?: string;
  commentCount: number;
  attachmentCount: number;
  projectId: number;
  projectName: string;
  createdAt: string;
  updatedAt: string;
}

// ── Comment ──
export interface Comment {
  id: number;
  content: string;
  author: User;
  createdAt: string;
  updatedAt: string;
}

// ── Label ──
export interface Label {
  id: number;
  name: string;
  color: string;
}

// ── Invitation ──
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED' | 'EXPIRED';

export interface Invitation {
  id: number;
  projectId: number;
  projectName: string;
  inviterName: string;
  inviterEmail: string;
  status: InvitationStatus;
  expiresAt: string;
  createdAt: string;
}

// ── Notification ──
export type NotificationType =
  | 'PROJECT_INVITED'
  | 'INVITATION_ACCEPTED'
  | 'TASK_ASSIGNED'
  | 'COMMENT_CREATED';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  body?: string;
  isRead: boolean;
  referenceId?: number;
  createdAt: string;
}

// ── Attachment ──
export interface Attachment {
  id: number;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  createdAt: string;
}
