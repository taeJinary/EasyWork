import { Routes, Route, Navigate } from 'react-router-dom';
import AppShell from '@/layouts/AppShell';
import AuthLayout from '@/layouts/AuthLayout';
import LoginPage from '@/pages/LoginPage';
import SignupPage from '@/pages/SignupPage';
import WorkspacesPage from '@/pages/WorkspacesPage';
import WorkspaceDetailPage from '@/pages/WorkspaceDetailPage';
import ProjectsPage from '@/pages/ProjectsPage';
import ProjectBoardPage from '@/pages/ProjectBoardPage';
import TaskListPage from '@/pages/TaskListPage';
import ProjectMembersPage from '@/pages/ProjectMembersPage';
import InvitationsPage from '@/pages/InvitationsPage';
import NotificationsPage from '@/pages/NotificationsPage';
import ProfileSettingsPage from '@/pages/ProfileSettingsPage';
import AccountSettingsPage from '@/pages/AccountSettingsPage';

export default function App() {
  return (
    <Routes>
      {/* Auth routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      {/* Authenticated routes */}
      <Route element={<AppShell />}>
        <Route path="/workspaces" element={<WorkspacesPage />} />
        <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        {/* Phase B — 핵심 생산성 화면 */}
        <Route path="/projects/:projectId/board" element={<ProjectBoardPage />} />
        <Route path="/projects/:projectId/tasks" element={<TaskListPage />} />
        {/* Phase C — 협업 화면 */}
        <Route path="/projects/:projectId/members" element={<ProjectMembersPage />} />
        <Route path="/invitations" element={<InvitationsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        {/* Phase D — 설정 */}
        <Route path="/projects/:projectId/settings" element={<div className="text-[var(--color-text-muted)]">프로젝트 설정 — 추후 구현 예정</div>} />
        <Route path="/settings/profile" element={<ProfileSettingsPage />} />
        <Route path="/settings/account" element={<AccountSettingsPage />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

