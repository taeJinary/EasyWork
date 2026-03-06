import type { ReactNode } from 'react';
import type { TaskStatus } from '@/types';
import { Circle, Loader, CheckCircle2 } from 'lucide-react';

interface BoardColumnProps {
  status: TaskStatus;
  count: number;
  children: ReactNode;
}

const statusConfig: Record<TaskStatus, { label: string; icon: typeof Circle; color: string }> = {
  TODO: { label: 'TODO', icon: Circle, color: 'var(--color-text-muted)' },
  IN_PROGRESS: { label: 'IN PROGRESS', icon: Loader, color: 'var(--color-warning)' },
  DONE: { label: 'DONE', icon: CheckCircle2, color: 'var(--color-success)' },
};

export default function BoardColumn({ status, count, children }: BoardColumnProps) {
  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <div className="flex-1 min-w-[280px] flex flex-col">
      {/* Column header */}
      <div className="
        flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-sm)] py-[var(--spacing-sm)]
        mb-[var(--spacing-sm)]
      ">
        <Icon size={14} style={{ color: config.color }} className={status === 'IN_PROGRESS' ? 'animate-spin' : ''} />
        <span className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
          {config.label}
        </span>
        <span className="
          text-[var(--text-xs)] text-[var(--color-text-muted)]
          bg-[var(--color-surface-muted)] rounded-full
          px-[6px] py-[1px] font-medium
        ">
          {count}
        </span>
      </div>

      {/* Cards container */}
      <div className="
        flex-1 flex flex-col gap-[var(--spacing-sm)]
        p-[var(--spacing-sm)] rounded-[var(--radius-md)]
        bg-[var(--color-surface-muted)] min-h-[200px]
      ">
        {children}
      </div>
    </div>
  );
}
