import { Routes, Route, Navigate } from 'react-router-dom';
import AppShell from '@/layouts/AppShell';
import AuthLayout from '@/layouts/AuthLayout';
import LoginPage from '@/pages/LoginPage';
import SignupPage from '@/pages/SignupPage';
import OAuthCallbackPage from '@/pages/OAuthCallbackPage';
import VerifyEmailPage from '@/pages/VerifyEmailPage';
import DashboardPage from '@/pages/DashboardPage';
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
import ProjectSettingsPage from '@/pages/ProjectSettingsPage';

export default function App() {
  return (
    <Routes>
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/oauth/google/callback" element={<OAuthCallbackPage provider="GOOGLE" />} />
      <Route path="/oauth/naver/callback" element={<OAuthCallbackPage provider="NAVER" />} />

      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      <Route element={<AppShell />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/workspaces" element={<WorkspacesPage />} />
        <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="/projects/:projectId/board" element={<ProjectBoardPage />} />
        <Route path="/projects/:projectId/tasks" element={<TaskListPage />} />
        <Route path="/projects/:projectId/members" element={<ProjectMembersPage />} />
        <Route path="/invitations" element={<InvitationsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/projects/:projectId/settings" element={<ProjectSettingsPage />} />
        <Route path="/settings/profile" element={<ProfileSettingsPage />} />
        <Route path="/settings/account" element={<AccountSettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
