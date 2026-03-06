import { Search } from 'lucide-react';
import type { ReactNode } from 'react';

interface FilterBarProps {
  searchPlaceholder?: string;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  children?: ReactNode;
}

export default function FilterBar({
  searchPlaceholder = '검색...',
  searchValue = '',
  onSearchChange,
  children,
}: FilterBarProps) {
  return (
    <div className="flex items-center gap-[var(--spacing-md)] py-[var(--spacing-md)]">
      <div className="relative flex-1 max-w-[320px]">
        <Search
          size={16}
          className="absolute left-[var(--spacing-sm)] top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
        />
        <input
          type="text"
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={(e) => onSearchChange?.(e.target.value)}
          className="
            w-full h-[32px] pl-[30px] pr-[var(--spacing-sm)]
            border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            placeholder:text-[var(--color-text-muted)]
            focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
          "
        />
      </div>
      {children && (
        <div className="flex items-center gap-[var(--spacing-sm)]">
          {children}
        </div>
      )}
    </div>
  );
}
