import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, Plus, Bell, ChevronDown, LogOut, Settings, User, Menu } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';

export default function GlobalHeader() {
  const { user, logout } = useAuthStore();
  const { toggleMobileSidebar } = useUiStore();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
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

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const displayName = user?.nickname?.trim() || 'User';
  const avatarInitial = displayName.charAt(0).toUpperCase() || 'U';

  return (
    <header className="
      h-[56px] bg-[var(--color-surface)] border-b border-[var(--color-border)]
      flex items-center px-[var(--spacing-base)] gap-[var(--spacing-base)]
      fixed top-0 left-0 right-0 z-50
    ">
      {/* Mobile Menu Button */}
      <button
        onClick={toggleMobileSidebar}
        className="
          md:hidden flex items-center justify-center w-[32px] h-[32px]
          text-[var(--color-text-secondary)] bg-transparent border-none cursor-pointer
          hover:bg-[var(--color-surface-muted)] rounded-[var(--radius-sm)] shrink-0
        "
        aria-label="Toggle Menu"
      >
        <Menu size={20} />
      </button>

      {/* Logo */}
      <Link
        to="/workspaces"
        className="text-[var(--text-base)] font-bold text-[var(--color-text-primary)] no-underline hover:no-underline shrink-0"
      >
        EasyWork
      </Link>

      {/* Workspace Switcher (placeholder) */}
      <button className="
        flex items-center gap-1 h-[32px] px-[var(--spacing-sm)]
        border border-[var(--color-border)] rounded-[var(--radius-sm)]
        bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
        hover:bg-[var(--color-surface-muted)] cursor-pointer
      ">
        <span className="max-w-[120px] truncate">Workspace</span>
        <ChevronDown size={14} />
      </button>

      {/* Global Search - Hidden on mobile */}
      <div className="relative hidden md:flex flex-1 max-w-[480px]">
        <Search
          size={16}
          className="absolute left-[var(--spacing-sm)] top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
        />
        <input
          type="text"
          placeholder="검색..."
          className="
            w-full h-[32px] pl-[30px] pr-[var(--spacing-sm)]
            border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface-muted)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            placeholder:text-[var(--color-text-muted)]
            focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
            focus:bg-[var(--color-surface)]
          "
        />
      </div>

      {/* Right actions */}
      <div className="flex items-center gap-[var(--spacing-sm)] ml-auto">
        {/* Quick create */}
        <button className="
          flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
          bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
          text-[var(--text-sm)] font-medium
          hover:bg-[var(--color-primary-hover)] cursor-pointer
          border-none
        ">
          <Plus size={16} />
          <span>New</span>
        </button>

        {/* Notifications */}
        <Link
          to="/notifications"
          className="
            relative flex items-center justify-center w-[32px] h-[32px]
            rounded-[var(--radius-sm)] text-[var(--color-text-secondary)]
            hover:bg-[var(--color-surface-muted)] no-underline
          "
        >
          <Bell size={18} />
        </Link>

        {/* Profile dropdown */}
        <div ref={profileRef} className="relative">
          <button
            onClick={() => setProfileOpen(!profileOpen)}
            className="
              flex items-center justify-center w-[32px] h-[32px]
              rounded-full bg-[var(--color-primary)] text-white
              text-[var(--text-xs)] font-semibold cursor-pointer
              border-none hover:opacity-90
            "
          >
            {avatarInitial}
          </button>

          {profileOpen && (
            <div className="
              absolute right-0 top-[40px] w-[200px]
              bg-[var(--color-surface)] border border-[var(--color-border)]
              rounded-[var(--radius-md)] shadow-[var(--shadow-sm)]
              py-[var(--spacing-xs)] z-50
            ">
              <div className="px-[var(--spacing-base)] py-[var(--spacing-sm)] border-b border-[var(--color-border)]">
                <div className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  {displayName}
                </div>
                <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] truncate">
                  {user?.email || ''}
                </div>
              </div>
              <Link
                to="/settings/profile"
                onClick={() => setProfileOpen(false)}
                className="
                  flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-sm)]
                  text-[var(--text-sm)] text-[var(--color-text-primary)] no-underline
                  hover:bg-[var(--color-surface-muted)]
                "
              >
                <User size={14} />
                프로필
              </Link>
              <Link
                to="/settings/profile"
                onClick={() => setProfileOpen(false)}
                className="
                  flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-sm)]
                  text-[var(--text-sm)] text-[var(--color-text-primary)] no-underline
                  hover:bg-[var(--color-surface-muted)]
                "
              >
                <Settings size={14} />
                설정
              </Link>
              <div className="border-t border-[var(--color-border)] mt-[var(--spacing-xs)]">
                <button
                  onClick={handleLogout}
                  className="
                    flex items-center gap-[var(--spacing-sm)] w-full px-[var(--spacing-base)] py-[var(--spacing-sm)]
                    text-[var(--text-sm)] text-[var(--color-danger)] bg-transparent border-none
                    cursor-pointer hover:bg-[var(--color-surface-muted)] text-left
                  "
                >
                  <LogOut size={14} />
                  로그아웃
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
