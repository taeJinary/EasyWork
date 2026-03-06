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
        {/* Placeholder routes for Phase C/D */}
        <Route path="/projects/:projectId/members" element={<div className="text-[var(--color-text-muted)]">멤버 관리 — Phase C에서 구현 예정</div>} />
        <Route path="/projects/:projectId/settings" element={<div className="text-[var(--color-text-muted)]">프로젝트 설정 — Phase D에서 구현 예정</div>} />
        <Route path="/invitations" element={<div className="text-[var(--color-text-muted)]">받은 초대 — Phase C에서 구현 예정</div>} />
        <Route path="/notifications" element={<div className="text-[var(--color-text-muted)]">알림 — Phase C에서 구현 예정</div>} />
        <Route path="/settings/profile" element={<div className="text-[var(--color-text-muted)]">프로필 설정 — Phase D에서 구현 예정</div>} />
        <Route path="/settings/account" element={<div className="text-[var(--color-text-muted)]">계정 설정 — Phase D에서 구현 예정</div>} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
