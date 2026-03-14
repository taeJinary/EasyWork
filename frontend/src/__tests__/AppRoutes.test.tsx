import { render, screen } from '@testing-library/react';
import { MemoryRouter, Outlet } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import App from '@/App';

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    isAuthenticated: true,
  }),
}));

vi.mock('@/layouts/AppShell', () => ({
  default: function MockAppShell() {
    return <Outlet />;
  },
}));

vi.mock('@/pages/LoginPage', () => ({ default: () => <div>login-page</div> }));
vi.mock('@/pages/SignupPage', () => ({ default: () => <div>signup-page</div> }));
vi.mock('@/pages/VerifyEmailPage', () => ({ default: () => <div>verify-email-page</div> }));
vi.mock('@/pages/OAuthCallbackPage', () => ({ default: () => <div>oauth-callback-page</div> }));
vi.mock('@/pages/DashboardPage', () => ({ default: () => <div>dashboard-page</div> }));
vi.mock('@/pages/WorkspacesPage', () => ({ default: () => <div>workspaces-page</div> }));
vi.mock('@/pages/WorkspaceDetailPage', () => ({ default: () => <div>workspace-detail-page</div> }));
vi.mock('@/pages/ProjectsPage', () => ({ default: () => <div>projects-page</div> }));
vi.mock('@/pages/ProjectBoardPage', () => ({ default: () => <div>project-board-page</div> }));
vi.mock('@/pages/TaskListPage', () => ({ default: () => <div>task-list-page</div> }));
vi.mock('@/pages/ProjectMembersPage', () => ({ default: () => <div>project-members-page</div> }));
vi.mock('@/pages/InvitationsPage', () => ({ default: () => <div>invitations-page</div> }));
vi.mock('@/pages/NotificationsPage', () => ({ default: () => <div>notifications-page</div> }));
vi.mock('@/pages/ProfileSettingsPage', () => ({ default: () => <div>profile-settings-page</div> }));
vi.mock('@/pages/AccountSettingsPage', () => ({ default: () => <div>account-settings-page</div> }));
vi.mock('@/pages/ProjectSettingsPage', () => ({ default: () => <div>project-settings-page</div> }));

describe('App routes', () => {
  it('renders verify-email route even when authenticated', () => {
    render(
      <MemoryRouter initialEntries={['/verify-email?token=abc']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText('verify-email-page')).toBeInTheDocument();
    expect(screen.queryByText('workspaces-page')).not.toBeInTheDocument();
  });

  it('renders oauth callback route even when authenticated', () => {
    render(
      <MemoryRouter initialEntries={['/oauth/google/callback?code=abc&state=123']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText('oauth-callback-page')).toBeInTheDocument();
    expect(screen.queryByText('workspaces-page')).not.toBeInTheDocument();
  });
});
