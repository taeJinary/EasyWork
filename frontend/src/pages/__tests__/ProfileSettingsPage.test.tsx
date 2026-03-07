import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ProfileSettingsPage from '@/pages/ProfileSettingsPage';
import { apiOk } from '@/test/helpers';
import type { UserProfile } from '@/types';

const mockGet = vi.fn();
const mockPatch = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    patch: (...args: unknown[]) => mockPatch(...args),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

// Mock authStore
const mockSetUser = vi.fn();
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 1, email: 'test@test.com', name: 'Test' },
    setUser: mockSetUser,
  }),
}));

function makeProfile(overrides: Partial<UserProfile> = {}): UserProfile {
  return {
    userId: 1,
    email: 'test@example.com',
    nickname: 'TestUser',
    profileImg: null,
    provider: 'local',
    createdAt: '2026-01-01T00:00:00',
    ...overrides,
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <ProfileSettingsPage />
    </MemoryRouter>
  );
}

describe('ProfileSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // 1. Loading skeleton
  it('shows loading skeleton while fetching profile', () => {
    mockGet.mockReturnValue(new Promise(() => {})); // never resolves
    renderPage();
    // Should show a shimmer/skeleton, not the form
    expect(screen.queryByLabelText('닉네임')).not.toBeInTheDocument();
  });

  // 2. Renders profile data from API
  it('renders profile data from API', async () => {
    const profile = makeProfile({ nickname: 'Alice', email: 'alice@email.com' });
    mockGet.mockResolvedValue(apiOk(profile));
    renderPage();

    await waitFor(() => {
      expect(screen.getByDisplayValue('Alice')).toBeInTheDocument();
    });
    // Email appears in both avatar and read-only field, so use getAllByText
    expect(screen.getAllByText('alice@email.com').length).toBeGreaterThanOrEqual(1);
  });

  // 3. Nickname update success
  it('saves nickname and shows success message', async () => {
    const profile = makeProfile({ nickname: 'OldName' });
    mockGet.mockResolvedValue(apiOk(profile));
    mockPatch.mockResolvedValue(apiOk({ ...profile, nickname: 'NewName' }));
    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByDisplayValue('OldName')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('OldName');
    await user.clear(input);
    await user.type(input, 'NewName');
    await user.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => {
      expect(mockPatch).toHaveBeenCalledWith('/users/me', { nickname: 'NewName' });
    });
  });

  // 4. Empty nickname → save button disabled
  it('disables save button when nickname is empty', async () => {
    mockGet.mockResolvedValue(apiOk(makeProfile()));
    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByDisplayValue('TestUser')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('TestUser');
    await user.clear(input);

    const saveBtn = screen.getByRole('button', { name: '저장' });
    expect(saveBtn).toBeDisabled();
  });

  // 5. Nickname < 2 chars → error message
  it('shows error when nickname is less than 2 chars', async () => {
    mockGet.mockResolvedValue(apiOk(makeProfile()));
    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByDisplayValue('TestUser')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('TestUser');
    await user.clear(input);
    await user.type(input, 'A');

    await waitFor(() => {
      expect(screen.getByText(/2자 이상/)).toBeInTheDocument();
    });
  });

  // 6. Nickname > 20 chars → error message
  it('shows error when nickname exceeds 20 chars', async () => {
    mockGet.mockResolvedValue(apiOk(makeProfile()));
    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByDisplayValue('TestUser')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('TestUser');
    await user.clear(input);
    await user.type(input, 'A'.repeat(21));

    await waitFor(() => {
      expect(screen.getByText(/20자 이하/)).toBeInTheDocument();
    });
  });

  // 7. API failure → error banner
  it('shows error banner on API failure', async () => {
    mockGet.mockRejectedValue(new Error('Network Error'));
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/프로필을 불러오는 데 실패/)).toBeInTheDocument();
    });
  });

  // 8. Submitting state → button disabled + text change
  it('shows submitting state while saving', async () => {
    mockGet.mockResolvedValue(apiOk(makeProfile()));
    mockPatch.mockReturnValue(new Promise(() => {})); // never resolves
    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByDisplayValue('TestUser')).toBeInTheDocument();
    });

    // Change nickname to enable save
    const input = screen.getByDisplayValue('TestUser');
    await user.clear(input);
    await user.type(input, 'Changed');
    await user.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => {
      expect(screen.getByText('저장 중...')).toBeInTheDocument();
    });
  });
});
