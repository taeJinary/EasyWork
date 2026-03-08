import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bell, ChevronDown, LogOut, Menu, Plus, Search, Settings, User } from 'lucide-react';
import apiClient from '@/api/client';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';
import type { ApiResponse, NotificationUnreadCount } from '@/types';

export default function GlobalHeader() {
  const { user, logout } = useAuthStore();
  const { toggleMobileSidebar } = useUiStore();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setProfileOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function fetchUnreadCount() {
      try {
        const response = await apiClient.get<ApiResponse<NotificationUnreadCount>>('/notifications/unread-count');
        if (isMounted) {
          setUnreadCount(response.data.data.unreadCount);
        }
      } catch {
        if (isMounted) {
          setUnreadCount(0);
        }
      }
    }

    if (user) {
      fetchUnreadCount();
    }

    return () => {
      isMounted = false;
    };
  }, [user]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const displayName = user?.nickname?.trim() || 'User';
  const avatarInitial = displayName.charAt(0).toUpperCase() || 'U';

  const handleCreateWorkspace = () => {
    navigate('/workspaces?create=workspace');
  };

  return (
    <header
      className="
        fixed left-0 right-0 top-0 z-50 flex h-[56px] items-center gap-[var(--spacing-base)]
        border-b border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-base)]
      "
    >
      <button
        onClick={toggleMobileSidebar}
        className="
          flex h-[32px] w-[32px] shrink-0 items-center justify-center rounded-[var(--radius-sm)]
          border-none bg-transparent text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] md:hidden
        "
        aria-label="Toggle Menu"
      >
        <Menu size={20} />
      </button>

      <Link
        to="/dashboard"
        className="shrink-0 text-[var(--text-base)] font-bold text-[var(--color-text-primary)] no-underline hover:no-underline"
      >
        EasyWork
      </Link>

      <button
        className="
          flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border border-[var(--color-border)]
          bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
          hover:bg-[var(--color-surface-muted)]
        "
      >
        <span className="max-w-[120px] truncate">Workspace</span>
        <ChevronDown size={14} />
      </button>

      <div className="relative hidden max-w-[480px] flex-1 md:flex">
        <Search
          size={16}
          className="absolute left-[var(--spacing-sm)] top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
        />
        <input
          type="text"
          placeholder="Search..."
          className="
            h-[32px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
            bg-[var(--color-surface-muted)] pl-[30px] pr-[var(--spacing-sm)] text-[var(--text-sm)]
            text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)]
            focus:bg-[var(--color-surface)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
          "
        />
      </div>

      <div className="ml-auto flex items-center gap-[var(--spacing-sm)]">
        <button
          onClick={handleCreateWorkspace}
          className="
            flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border-none
            bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
            hover:bg-[var(--color-primary-hover)]
          "
        >
          <Plus size={16} />
          <span>New</span>
        </button>

        <Link
          to="/notifications"
          aria-label="Notifications"
          className="
            relative flex h-[32px] w-[32px] items-center justify-center rounded-[var(--radius-sm)]
            text-[var(--color-text-secondary)] no-underline hover:bg-[var(--color-surface-muted)]
          "
        >
          <Bell size={18} />
          {unreadCount > 0 && (
            <span
              className="
                absolute -right-[4px] -top-[4px] min-w-[18px] rounded-full
                bg-[var(--color-danger)] px-[4px] py-[1px] text-center
                text-[10px] font-bold leading-none text-white
              "
            >
              {unreadCount}
            </span>
          )}
        </Link>

        <div ref={profileRef} className="relative">
          <button
            onClick={() => setProfileOpen((prev) => !prev)}
            className="
              flex h-[32px] w-[32px] items-center justify-center rounded-full border-none
              bg-[var(--color-primary)] text-[var(--text-xs)] font-semibold text-white hover:opacity-90
            "
          >
            {avatarInitial}
          </button>

          {profileOpen && (
            <div
              className="
                absolute right-0 top-[40px] z-50 w-[200px] rounded-[var(--radius-md)]
                border border-[var(--color-border)] bg-[var(--color-surface)] py-[var(--spacing-xs)] shadow-[var(--shadow-sm)]
              "
            >
              <div className="border-b border-[var(--color-border)] px-[var(--spacing-base)] py-[var(--spacing-sm)]">
                <div className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  {displayName}
                </div>
                <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                  {user?.email || ''}
                </div>
              </div>
              <Link
                to="/settings/profile"
                onClick={() => setProfileOpen(false)}
                className="
                  flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-sm)]
                  text-[var(--text-sm)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]
                "
              >
                <User size={14} />
                Profile
              </Link>
              <Link
                to="/settings/profile"
                onClick={() => setProfileOpen(false)}
                className="
                  flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-sm)]
                  text-[var(--text-sm)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]
                "
              >
                <Settings size={14} />
                Settings
              </Link>
              <div className="mt-[var(--spacing-xs)] border-t border-[var(--color-border)]">
                <button
                  onClick={handleLogout}
                  className="
                    flex w-full items-center gap-[var(--spacing-sm)] border-none bg-transparent
                    px-[var(--spacing-base)] py-[var(--spacing-sm)] text-left text-[var(--text-sm)]
                    text-[var(--color-danger)] hover:bg-[var(--color-surface-muted)]
                  "
                >
                  <LogOut size={14} />
                  Logout
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
