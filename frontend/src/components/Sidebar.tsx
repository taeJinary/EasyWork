import { NavLink } from 'react-router-dom';
import {
  Home,
  Layers,
  FolderKanban,
  Mail,
  Bell,
  Settings,
  FolderOpen,
} from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';

const mainMenuItems = [
  { to: '/dashboard', label: 'Home', icon: Home },
  { to: '/workspaces', label: 'Workspaces', icon: Layers },
  { to: '/projects', label: 'Projects', icon: FolderKanban },
  { to: '/invitations', label: 'Invitations', icon: Mail },
  { to: '/notifications', label: 'Notifications', icon: Bell },
  { to: '/settings/profile', label: 'Settings', icon: Settings },
];

const recentProjects = [
  { id: 1, name: 'EasyWork' },
  { id: 2, name: 'Internal Tools' },
  { id: 3, name: 'Design System' },
];

export default function Sidebar() {
  const { isMobileSidebarOpen, setMobileSidebarOpen } = useUiStore();

  return (
    <>
      {isMobileSidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={() => setMobileSidebarOpen(false)}
        />
      )}

      <aside
        className={`
          fixed left-0 top-[56px] z-50 flex h-[calc(100vh-56px)] w-[256px]
          flex-col overflow-y-auto border-r border-[var(--color-border)] bg-[var(--color-surface)]
          transition-transform duration-200 ease-in-out
          ${isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'}
          md:translate-x-0
        `}
      >
        <nav className="flex-1 py-[var(--spacing-sm)]">
          <ul className="m-0 list-none p-0">
            {mainMenuItems.map((item) => (
              <li key={item.label}>
                <NavLink
                  to={item.to}
                  onClick={() => setMobileSidebarOpen(false)}
                  className={({ isActive }) => `
                    flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[7px]
                    text-[var(--text-sm)] no-underline transition-colors
                    ${isActive
                      ? 'bg-[var(--color-surface-muted)] text-[var(--color-text-primary)] font-semibold'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]'
                    }
                  `}
                >
                  <item.icon size={16} className="shrink-0" />
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="border-t border-[var(--color-border)] py-[var(--spacing-sm)]">
          <div className="px-[var(--spacing-base)] py-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
            Recent Projects
          </div>
          <ul className="m-0 list-none p-0">
            {recentProjects.map((project) => (
              <li key={project.id}>
                <NavLink
                  to={`/projects/${project.id}/board`}
                  onClick={() => setMobileSidebarOpen(false)}
                  className={({ isActive }) => `
                    flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[6px]
                    text-[var(--text-sm)] no-underline
                    ${isActive
                      ? 'text-[var(--color-text-primary)] font-medium'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]'
                    }
                  `}
                >
                  <FolderOpen size={14} className="shrink-0 text-[var(--color-text-muted)]" />
                  <span className="truncate">{project.name}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      </aside>
    </>
  );
}
