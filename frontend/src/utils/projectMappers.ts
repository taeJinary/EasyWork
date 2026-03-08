import type {
  ProjectDetail,
  ProjectDetailResponse,
  ProjectListItemResponse,
  ProjectSummary,
} from '@/types';

export function toProjectSummary(project: ProjectListItemResponse): ProjectSummary {
  return {
    id: project.projectId,
    name: project.name,
    description: project.description,
    myRole: project.role,
    memberCount: project.memberCount,
    openTaskCount: Math.max(project.taskCount - project.doneTaskCount, 0),
    taskCount: project.taskCount,
    doneTaskCount: project.doneTaskCount,
    progressRate: project.progressRate,
    updatedAt: project.updatedAt,
  };
}

export function toProjectDetail(project: ProjectDetailResponse): ProjectDetail {
  return {
    id: project.projectId,
    name: project.name,
    description: project.description,
    myRole: project.myRole,
    memberCount: project.memberCount,
    pendingInvitationCount: project.pendingInvitationCount,
    taskSummary: project.taskSummary,
    members: project.members,
  };
}
