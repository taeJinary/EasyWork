import type { ReactNode } from 'react';

interface BadgeProps {
  children: ReactNode;
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'muted';
  size?: 'sm' | 'md';
}

const variantClasses: Record<string, string> = {
  default: 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]',
  primary: 'bg-[var(--color-accent-blue)] text-[var(--color-primary)]',
  success: 'bg-[var(--color-accent-green)] text-[var(--color-success)]',
  warning: 'bg-[var(--color-accent-yellow)] text-[var(--color-warning)]',
  danger: 'bg-[var(--color-accent-red)] text-[var(--color-danger)]',
  muted: 'bg-[var(--color-surface-muted)] text-[var(--color-text-muted)]',
};

export default function Badge({ children, variant = 'default', size = 'sm' }: BadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center font-medium rounded-[var(--radius-sm)]
        ${size === 'sm' ? 'px-[6px] py-[2px] text-[var(--text-xs)]' : 'px-[8px] py-[3px] text-[var(--text-sm)]'}
        ${variantClasses[variant]}
      `}
    >
      {children}
    </span>
  );
}
