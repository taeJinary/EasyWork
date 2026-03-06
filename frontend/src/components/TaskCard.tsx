import type { BoardTaskCard } from '@/types';
import Badge from '@/components/Badge';
import { MessageSquare, Calendar } from 'lucide-react';

interface TaskCardProps {
  task: BoardTaskCard;
  onClick?: () => void;
}

const priorityVariant: Record<string, 'danger' | 'warning' | 'primary' | 'muted'> = {
  URGENT: 'danger',
  HIGH: 'warning',
  MEDIUM: 'primary',
  LOW: 'muted',
};

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  const month = d.toLocaleString('en', { month: 'short' });
  return `${month} ${d.getDate()}`;
}

export default function TaskCard({ task, onClick }: TaskCardProps) {
  return (
    <div
      onClick={onClick}
      className="
        bg-[var(--color-surface)] border border-[var(--color-border)]
        rounded-[var(--radius-sm)] p-[var(--spacing-md)]
        cursor-pointer hover:border-[var(--color-primary)] transition-colors
      "
    >
      {/* Title */}
      <div className="text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)] leading-snug">
        {task.title}
      </div>

      {/* Labels */}
      {task.labels.length > 0 && (
        <div className="flex flex-wrap gap-1 mb-[var(--spacing-sm)]">
          {task.labels.slice(0, 3).map((label) => (
            <span
              key={label.labelId}
              className="
                inline-block px-[5px] py-[1px] text-[11px] font-medium
                rounded-[var(--radius-sm)] border
              "
              style={{
                backgroundColor: `${label.colorHex}20`,
                borderColor: `${label.colorHex}40`,
                color: label.colorHex,
              }}
            >
              {label.name}
            </span>
          ))}
          {task.labels.length > 3 && (
            <span className="text-[11px] text-[var(--color-text-muted)]">
              +{task.labels.length - 3}
            </span>
          )}
        </div>
      )}

      {/* Bottom meta row */}
      <div className="flex items-center justify-between mt-[var(--spacing-xs)]">
        <div className="flex items-center gap-[var(--spacing-sm)]">
          {/* Priority */}
          <Badge variant={priorityVariant[task.priority] || 'muted'} size="sm">
            {task.priority}
          </Badge>
        </div>

        <div className="flex items-center gap-[var(--spacing-md)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
          {/* Due date */}
          {task.dueDate && (
            <span className="flex items-center gap-[2px]">
              <Calendar size={11} />
              {formatDate(task.dueDate)}
            </span>
          )}
          {/* Comments */}
          {task.commentCount > 0 && (
            <span className="flex items-center gap-[2px]">
              <MessageSquare size={11} />
              {task.commentCount}
            </span>
          )}
        </div>
      </div>

      {/* Assignee row */}
      {task.assignee && (
        <div className="flex items-center gap-[var(--spacing-xs)] mt-[var(--spacing-sm)] pt-[var(--spacing-xs)] border-t border-[var(--color-border-muted)]">
          <div className="
            w-[18px] h-[18px] rounded-full bg-[var(--color-primary)]
            text-white text-[10px] font-semibold
            flex items-center justify-center shrink-0
          ">
            {task.assignee.nickname.charAt(0).toUpperCase()}
          </div>
          <span className="text-[var(--text-xs)] text-[var(--color-text-secondary)] truncate">
            {task.assignee.nickname}
          </span>
        </div>
      )}
    </div>
  );
}
