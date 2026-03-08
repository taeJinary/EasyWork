import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ProjectSettingsPage from '@/pages/ProjectSettingsPage';
import { apiOk } from '@/test/helpers';
import type { ProjectDetailResponse } from '@/types';

const mockGet = vi.fn();
const mockPatch = vi.fn();
const mockDelete = vi.fn();
const mockNavigate = vi.fn();

let currentProjectId = '1';

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    patch: (...args: unknown[]) => mockPatch(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
    post: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ projectId: currentProjectId }),
    useNavigate: () => mockNavigate,
  };
});

function makeProject(overrides: Partial<ProjectDetailResponse> = {}): ProjectDetailResponse {
  return {
    projectId: 1,
    name: 'Project Alpha',
    description: 'Alpha description',
    myRole: 'OWNER',
    memberCount: 3,
    pendingInvitationCount: 1,
    taskSummary: {
      todo: 2,
      inProgress: 1,
      done: 3,
    },
    members: [],
    ...overrides,
  };
}

function createWrapper(queryClient: QueryClient) {
  return (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ProjectSettingsPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  const ui = createWrapper(queryClient);

  return {
    queryClient,
    ui,
    ...render(ui),
  };
}

describe('ProjectSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    currentProjectId = '1';

    mockGet.mockImplementation((url: string) => {
      if (url === '/projects/1') {
        return Promise.resolve(apiOk(makeProject()));
      }

      if (url === '/projects/2') {
        return Promise.resolve(
          apiOk(
            makeProject({
              projectId: 2,
              name: 'Project Beta',
              description: 'Beta description',
            })
          )
        );
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
  });

  it('resets local draft fields when projectId changes', async () => {
    const view = renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByDisplayValue('Project Alpha')).toBeInTheDocument();
    });

    const nameInput = screen.getByDisplayValue('Project Alpha');
    await user.clear(nameInput);
    await user.type(nameInput, 'Alpha draft');

    await waitFor(() => {
      expect(screen.getByDisplayValue('Alpha draft')).toBeInTheDocument();
    });

    currentProjectId = '2';
    view.rerender(createWrapper(view.queryClient));

    await waitFor(() => {
      expect(screen.getByDisplayValue('Project Beta')).toBeInTheDocument();
    });

    expect(screen.queryByDisplayValue('Alpha draft')).not.toBeInTheDocument();
  });

  it('shows a delete error message when project deletion fails', async () => {
    mockDelete.mockRejectedValue(new Error('delete failed'));
    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Delete project' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Delete project' }));

    const textboxes = screen.getAllByRole('textbox');
    const confirmInput = textboxes[textboxes.length - 1];

    await user.type(confirmInput, 'Project Alpha');
    await user.click(
      screen.getByRole('button', {
        name: 'I understand the consequences, delete this project',
      })
    );

    await waitFor(() => {
      expect(screen.getByText(/삭제에 실패했습니다/)).toBeInTheDocument();
    });
  });
});
