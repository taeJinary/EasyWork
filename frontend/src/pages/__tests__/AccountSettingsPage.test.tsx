import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AccountSettingsPage from '@/pages/AccountSettingsPage';
import { apiOk } from '@/test/helpers';

const mockPatch = vi.fn();
const mockDelete = vi.fn();
const mockGet = vi.fn();
const mockPost = vi.fn();
const mockIsWebPushConfigured = vi.fn();
const mockIssueWebPushToken = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
    patch: (...args: unknown[]) => mockPatch(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
  },
}));

vi.mock('@/push/webPush', () => ({
  isWebPushConfigured: () => mockIsWebPushConfigured(),
  issueWebPushToken: (...args: unknown[]) => mockIssueWebPushToken(...args),
}));

const mockLogout = vi.fn();
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    logout: mockLogout,
  }),
}));

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

function setNotificationPermission(permission: NotificationPermission) {
  Object.defineProperty(window, 'Notification', {
    configurable: true,
    writable: true,
    value: { permission },
  });
}

function setServiceWorkerSupport(supported: boolean) {
  if (supported) {
    Object.defineProperty(window.navigator, 'serviceWorker', {
      configurable: true,
      writable: true,
      value: {},
    });
    return;
  }

  Reflect.deleteProperty(window.navigator, 'serviceWorker');
}

describe('AccountSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGet.mockResolvedValue(apiOk([]));
    mockIsWebPushConfigured.mockReturnValue(true);
    mockIssueWebPushToken.mockResolvedValue('web-token-issued');
    setNotificationPermission('granted');
    setServiceWorkerSupport(true);
  });

  it('renders password change form fields', async () => {
    renderPage();
    expect(screen.getByLabelText('현재 비밀번호')).toBeInTheDocument();
    expect(screen.getByLabelText('새 비밀번호')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '비밀번호 변경' })).toBeInTheDocument();
    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/notifications/push-tokens');
    });
  });

  it('disables change button when fields are empty', () => {
    renderPage();
    const changeBtn = screen.getByRole('button', { name: '비밀번호 변경' });
    expect(changeBtn).toBeDisabled();
  });

  it('shows error when new password has no special character', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('현재 비밀번호'), 'OldPass1!');
    await user.type(screen.getByLabelText('새 비밀번호'), 'NewPass123');

    await waitFor(() => {
      expect(screen.getByText(/모두 포함해야/)).toBeInTheDocument();
    });
  });

  it('shows error when new password is less than 8 chars', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('현재 비밀번호'), 'OldPass1!');
    await user.type(screen.getByLabelText('새 비밀번호'), 'Ab1!');

    await waitFor(() => {
      expect(screen.getByText(/8자 이상/)).toBeInTheDocument();
    });
  });

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

  it('shows withdraw confirmation modal on button click', async () => {
    renderPage();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: '회원 탈퇴' }));

    await waitFor(() => {
      expect(screen.getByText(/정말 탈퇴/)).toBeInTheDocument();
      expect(screen.getByLabelText('비밀번호 확인')).toBeInTheDocument();
    });
  });

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

  it('registers a non-web push token and shows registered device info', async () => {
    mockGet.mockResolvedValueOnce(apiOk([]));
    mockPost.mockResolvedValue(
      apiOk({
        token: 'android-token-123',
        platform: 'ANDROID',
        active: true,
      })
    );

    renderPage();
    const user = userEvent.setup();

    await user.selectOptions(screen.getByLabelText('플랫폼'), 'ANDROID');
    await user.type(screen.getByLabelText('푸시 토큰'), 'android-token-123');
    await user.click(screen.getByRole('button', { name: '디바이스 등록' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/notifications/push-tokens', {
        token: 'android-token-123',
        platform: 'ANDROID',
      });
    });

    expect(await screen.findByText('활성 디바이스가 등록되었습니다.')).toBeInTheDocument();
    const registeredSection = screen.getByTestId('registered-device-list');
    expect(within(registeredSection).getByText('ANDROID')).toBeInTheDocument();
    expect(within(registeredSection).getByText('android-token-123')).toBeInTheDocument();
  });

  it('uses browser-issued web push token instead of manual input for WEB', async () => {
    mockGet.mockResolvedValueOnce(apiOk([]));
    mockIssueWebPushToken.mockResolvedValueOnce('web-token-issued');
    mockPost.mockResolvedValue(
      apiOk({
        token: 'web-token-issued',
        platform: 'WEB',
        active: true,
      })
    );

    renderPage();
    const user = userEvent.setup();

    expect(screen.queryByLabelText('푸시 토큰')).not.toBeInTheDocument();
    expect(screen.getByText('브라우저에서 발급된 푸시 토큰으로 현재 디바이스를 등록합니다.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '디바이스 등록' }));

    await waitFor(() => {
      expect(mockIssueWebPushToken).toHaveBeenCalled();
      expect(mockPost).toHaveBeenCalledWith('/notifications/push-tokens', {
        token: 'web-token-issued',
        platform: 'WEB',
      });
    });
  });

  it('unregisters the currently registered push token', async () => {
    mockGet.mockResolvedValueOnce(apiOk([]));
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

    await user.selectOptions(screen.getByLabelText('플랫폼'), 'ANDROID');
    await user.type(screen.getByLabelText('푸시 토큰'), 'android-token-9');
    await user.click(screen.getByRole('button', { name: '디바이스 등록' }));

    expect(await screen.findByText('등록된 디바이스')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '디바이스 해제 android-token-9' }));

    await waitFor(() => {
      expect(mockDelete).toHaveBeenCalledWith('/notifications/push-tokens', {
        params: { token: 'android-token-9' },
      });
    });

    expect(await screen.findByText('디바이스 등록이 해제되었습니다.')).toBeInTheDocument();
    expect(screen.queryByText('등록된 디바이스')).not.toBeInTheDocument();
  });

  it('loads registered devices on page open', async () => {
    mockGet.mockResolvedValueOnce(apiOk([
      { token: 'web-token-1', platform: 'WEB', active: true },
      { token: 'ios-token-2', platform: 'IOS', active: true },
    ]));

    renderPage();

    expect(await screen.findByText('등록된 디바이스')).toBeInTheDocument();
    const registeredSection = screen.getByTestId('registered-device-list');
    expect(within(registeredSection).getByText('web-token-1')).toBeInTheDocument();
    expect(within(registeredSection).getByText('ios-token-2')).toBeInTheDocument();
  });

  it('keeps multiple registered devices visible and unregisters only the selected token', async () => {
    mockGet.mockResolvedValueOnce(apiOk([
      { token: 'web-token-1', platform: 'WEB', active: true },
      { token: 'android-token-2', platform: 'ANDROID', active: true },
    ]));
    mockDelete.mockResolvedValue(apiOk({ removed: true }));

    renderPage();
    const user = userEvent.setup();

    expect(await screen.findByText('등록된 디바이스')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '디바이스 해제 web-token-1' }));

    await waitFor(() => {
      expect(mockDelete).toHaveBeenCalledWith('/notifications/push-tokens', {
        params: { token: 'web-token-1' },
      });
    });

    expect(screen.queryByText('web-token-1')).not.toBeInTheDocument();
    expect(screen.getByText('android-token-2')).toBeInTheDocument();
  });

  it('shows empty state when there are no registered devices', async () => {
    mockGet.mockResolvedValueOnce(apiOk([]));

    renderPage();

    expect(await screen.findByText('등록된 디바이스가 없습니다.')).toBeInTheDocument();
    expect(screen.getByText('새 토큰을 등록하면 이 기기에서 알림을 받을 수 있습니다.')).toBeInTheDocument();
  });

  it('shows retry action when loading registered devices fails', async () => {
    mockGet
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(apiOk([{ token: 'web-token-1', platform: 'WEB', active: true }]));

    renderPage();
    const user = userEvent.setup();

    expect(await screen.findByText('등록된 디바이스 목록을 불러오지 못했습니다.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '다시 시도' }));

    expect(await screen.findByText('등록된 디바이스')).toBeInTheDocument();
    expect(screen.getByText('web-token-1')).toBeInTheDocument();
  });

  it('shows newly registered device even when initial device load failed', async () => {
    mockGet.mockRejectedValueOnce(new Error('load failed'));
    mockPost.mockResolvedValue(
      apiOk({
        token: 'android-token-9',
        platform: 'ANDROID',
        active: true,
      })
    );

    renderPage();
    const user = userEvent.setup();

    expect(await screen.findByText('등록된 디바이스 목록을 불러오지 못했습니다.')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('플랫폼'), 'ANDROID');
    await user.type(screen.getByLabelText('푸시 토큰'), 'android-token-9');
    await user.click(screen.getByRole('button', { name: '디바이스 등록' }));

    expect(await screen.findByText('활성 디바이스가 등록되었습니다.')).toBeInTheDocument();
    expect(screen.getByText('등록된 디바이스')).toBeInTheDocument();
    expect(screen.getByText('android-token-9')).toBeInTheDocument();
    expect(screen.queryByText('등록된 디바이스 목록을 불러오지 못했습니다.')).not.toBeInTheDocument();
  });

  it('shows permission guidance when web notification permission is not granted', async () => {
    setNotificationPermission('default');

    renderPage();

    expect(
      await screen.findByText('브라우저 알림 권한이 아직 허용되지 않았습니다. 권한을 허용하지 않으면 웹 푸시를 받을 수 없습니다.')
    ).toBeInTheDocument();
  });

  it('disables web registration when notification permission is denied', async () => {
    setNotificationPermission('denied');

    renderPage();

    expect(screen.getByRole('button', { name: '디바이스 등록' })).toBeDisabled();
    expect(
      await screen.findByText('현재 환경에서는 WEB 디바이스 등록이 비활성화됩니다. 권한 또는 브라우저 지원 상태를 먼저 해결하세요.')
    ).toBeInTheDocument();
  });

  it('shows support guidance when service worker support is missing for web push', async () => {
    setServiceWorkerSupport(false);

    renderPage();

    expect(
      await screen.findByText('현재 브라우저는 웹 푸시 수신 환경을 완전히 지원하지 않을 수 있습니다. 토큰 등록 전 브라우저와 서비스 워커 설정을 확인하세요.')
    ).toBeInTheDocument();
  });

  it('disables web registration when service worker support is missing', async () => {
    setServiceWorkerSupport(false);

    renderPage();

    expect(screen.getByRole('button', { name: '디바이스 등록' })).toBeDisabled();
    expect(
      await screen.findByText('현재 환경에서는 WEB 디바이스 등록이 비활성화됩니다. 권한 또는 브라우저 지원 상태를 먼저 해결하세요.')
    ).toBeInTheDocument();
  });

  it('keeps non-web registration available even when web permission is denied', async () => {
    setNotificationPermission('denied');

    renderPage();
    const user = userEvent.setup();

    await user.selectOptions(screen.getByLabelText('플랫폼'), 'ANDROID');
    await user.type(screen.getByLabelText('푸시 토큰'), 'android-token-1');

    expect(screen.getByRole('button', { name: '디바이스 등록' })).toBeEnabled();
  });

  it('disables web registration when web push config is missing', async () => {
    mockIsWebPushConfigured.mockReturnValue(false);

    renderPage();

    expect(screen.getByRole('button', { name: '디바이스 등록' })).toBeDisabled();
    expect(screen.getByText('웹 푸시 설정이 준비되지 않아 현재 브라우저 디바이스를 등록할 수 없습니다.')).toBeInTheDocument();
  });
});
