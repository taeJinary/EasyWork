import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export default function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="flex items-start justify-between pb-[var(--spacing-base)] border-b border-[var(--color-border)]">
      <div>
        <h1 className="text-[var(--text-xl)] font-bold text-[var(--color-text-primary)] m-0">
          {title}
        </h1>
        {description && (
          <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mt-[var(--spacing-xs)] m-0">
            {description}
          </p>
        )}
      </div>
      {actions && (
        <div className="flex items-center gap-[var(--spacing-sm)]">
          {actions}
        </div>
      )}
    </div>
  );
}
