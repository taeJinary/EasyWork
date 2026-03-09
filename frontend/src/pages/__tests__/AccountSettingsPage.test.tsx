import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AccountSettingsPage from '@/pages/AccountSettingsPage';
import { apiOk } from '@/test/helpers';

const mockPatch = vi.fn();
const mockDelete = vi.fn();
const mockPost = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: (...args: unknown[]) => mockPost(...args),
    patch: (...args: unknown[]) => mockPatch(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
  },
}));

// Mock authStore — capture logout calls
const mockLogout = vi.fn();
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    logout: mockLogout,
  }),
}));

// Mock navigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function renderPage() {
  return render(
    <MemoryRouter>
      <AccountSettingsPage />
    </MemoryRouter>
  );
}

describe('AccountSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // 1. Renders password change form
  it('renders password change form fields', () => {
    renderPage();
    expect(screen.getByLabelText('현재 비밀번호')).toBeInTheDocument();
    expect(screen.getByLabelText('새 비밀번호')).toBeInTheDocument();
    // "비밀번호 변경" appears in both h2 title and button — use getByRole for the button
    expect(screen.getByRole('button', { name: '비밀번호 변경' })).toBeInTheDocument();
  });

  // 2. Empty fields → change button disabled
  it('disables change button when fields are empty', () => {
    renderPage();
    const changeBtn = screen.getByRole('button', { name: '비밀번호 변경' });
    expect(changeBtn).toBeDisabled();
  });

  // 3. Invalid new password (no special char) → error
  it('shows error when new password has no special character', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('현재 비밀번호'), 'OldPass1!');
    await user.type(screen.getByLabelText('새 비밀번호'), 'NewPass123');

    await waitFor(() => {
      // Validation error text is more specific than the hint text
      expect(screen.getByText(/모두 포함해야/)).toBeInTheDocument();
    });
  });

  // 4. New password < 8 chars → error
  it('shows error when new password is less than 8 chars', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('현재 비밀번호'), 'OldPass1!');
    await user.type(screen.getByLabelText('새 비밀번호'), 'Ab1!');

    await waitFor(() => {
      expect(screen.getByText(/8자 이상/)).toBeInTheDocument();
    });
  });

  // 5. Valid password → change success
  it('successfully changes password with valid input', async () => {
    mockPatch.mockResolvedValue(apiOk(null, '비밀번호가 변경되었습니다.'));
    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('현재 비밀번호'), 'OldPass1!');
    await user.type(screen.getByLabelText('새 비밀번호'), 'NewPass1!');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(mockPatch).toHaveBeenCalledWith('/users/me/password', {
        currentPassword: 'OldPass1!',
        newPassword: 'NewPass1!',
      });
    });

    await waitFor(() => {
      expect(screen.getByText(/비밀번호가 변경/)).toBeInTheDocument();
    });
  });

  // 6. Password change API failure → error banner
  it('shows error on password change failure', async () => {
    mockPatch.mockRejectedValue({ response: { data: { message: '현재 비밀번호가 틀렸습니다.' } } });
    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('현재 비밀번호'), 'WrongPass1!');
    await user.type(screen.getByLabelText('새 비밀번호'), 'NewPass1!');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(screen.getByText(/현재 비밀번호가 틀렸습니다/)).toBeInTheDocument();
    });
  });

  // 7. Withdraw button → shows confirmation modal
  it('shows withdraw confirmation modal on button click', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: '회원 탈퇴' }));

    await waitFor(() => {
      expect(screen.getByText(/정말 탈퇴/)).toBeInTheDocument();
      expect(screen.getByLabelText('비밀번호 확인')).toBeInTheDocument();
    });
  });

  // 8. Withdraw success → logout + redirect
  it('successfully withdraws, logs out, and redirects to login', async () => {
    mockDelete.mockResolvedValue(apiOk(null, '회원 탈퇴가 완료되었습니다.'));
    renderPage();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: '회원 탈퇴' }));

    await waitFor(() => {
      expect(screen.getByLabelText('비밀번호 확인')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('비밀번호 확인'), 'MyPass1!');
    await user.click(screen.getByRole('button', { name: '탈퇴 확인' }));

    await waitFor(() => {
      expect(mockDelete).toHaveBeenCalledWith('/users/me', { data: { password: 'MyPass1!' } });
    });

    await waitFor(() => {
      expect(mockLogout).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
    });
  });

  // 9. Withdraw API failure → error
  it('shows error on withdraw failure', async () => {
    mockDelete.mockRejectedValue(new Error('Network Error'));
    renderPage();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: '회원 탈퇴' }));

    await waitFor(() => {
      expect(screen.getByLabelText('비밀번호 확인')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('비밀번호 확인'), 'MyPass1!');
    await user.click(screen.getByRole('button', { name: '탈퇴 확인' }));

    await waitFor(() => {
      expect(screen.getByText(/탈퇴에 실패/)).toBeInTheDocument();
    });
  });

  // 10. Cancel withdraw modal → modal closes
  it('closes withdraw modal on cancel', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: '회원 탈퇴' }));

    await waitFor(() => {
      expect(screen.getByText(/정말 탈퇴/)).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: '취소' }));

    await waitFor(() => {
      expect(screen.queryByText(/정말 탈퇴/)).not.toBeInTheDocument();
    });
  });

  it('registers a push token and shows registered device info', async () => {
    mockPost.mockResolvedValue(
      apiOk({
        token: 'web-token-123',
        platform: 'WEB',
        active: true,
      })
    );

    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('푸시 토큰'), 'web-token-123');
    await user.selectOptions(screen.getByLabelText('플랫폼'), 'WEB');
    await user.click(screen.getByRole('button', { name: '디바이스 등록' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/notifications/push-tokens', {
        token: 'web-token-123',
        platform: 'WEB',
      });
    });

    expect(await screen.findByText('활성 디바이스가 등록되었습니다.')).toBeInTheDocument();
    const registeredSection = screen.getByText('등록된 디바이스').closest('div');
    expect(registeredSection).not.toBeNull();
    expect(within(registeredSection as HTMLElement).getByText('WEB')).toBeInTheDocument();
    expect(within(registeredSection as HTMLElement).getByText('web-token-123')).toBeInTheDocument();
  });

  it('unregisters the currently registered push token', async () => {
    mockPost.mockResolvedValue(
      apiOk({
        token: 'android-token-9',
        platform: 'ANDROID',
        active: true,
      })
    );
    mockDelete.mockResolvedValue(apiOk({ removed: true }));

    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('푸시 토큰'), 'android-token-9');
    await user.selectOptions(screen.getByLabelText('플랫폼'), 'ANDROID');
    await user.click(screen.getByRole('button', { name: '디바이스 등록' }));

    expect(await screen.findByText('등록된 디바이스')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '디바이스 해제' }));

    await waitFor(() => {
      expect(mockDelete).toHaveBeenCalledWith('/notifications/push-tokens', {
        params: { token: 'android-token-9' },
      });
    });

    expect(await screen.findByText('디바이스 등록이 해제되었습니다.')).toBeInTheDocument();
    expect(screen.queryByText('등록된 디바이스')).not.toBeInTheDocument();
  });
});
