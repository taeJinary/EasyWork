import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import Sidebar from '@/components/Sidebar';
import { useUiStore } from '@/stores/uiStore';

describe('Sidebar', () => {
  beforeEach(() => {
    useUiStore.setState({ isMobileSidebarOpen: false });
  });

  it('routes Home navigation to dashboard', () => {
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: /home/i })).toHaveAttribute('href', '/dashboard');
  });
});
