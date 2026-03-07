import { AlertCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
  showHomeButton?: boolean;
}

export default function ErrorState({
  title = '오류가 발생했습니다.',
  message = '페이지를 불러오는데 문제가 생겼거나, 접근 권한이 없습니다.',
  onRetry,
  showHomeButton = true,
}: ErrorStateProps) {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center p-[var(--spacing-xl)] text-center min-h-[400px]">
      <div className="
        w-[48px] h-[48px] rounded-full bg-[var(--color-accent-red)]
        flex items-center justify-center mb-[var(--spacing-md)] mx-auto
      ">
        <AlertCircle size={24} className="text-[var(--color-danger)]" />
      </div>
      
      <h3 className="text-[var(--text-lg)] font-semibold text-[var(--color-text-primary)] mb-[var(--spacing-xs)] m-0">
        {title}
      </h3>
      
      <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mb-[var(--spacing-lg)] max-w-[400px]">
        {message}
      </p>
      
      <div className="flex items-center justify-center gap-[var(--spacing-sm)]">
        {onRetry && (
          <button
            onClick={onRetry}
            className="
              h-[32px] px-[var(--spacing-md)]
              bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-sm)]
              text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] cursor-pointer
              hover:bg-[var(--color-surface-muted)]
            "
          >
            다시 시도
          </button>
        )}
        
        {showHomeButton && (
          <button
            onClick={() => navigate('/projects')}
            className="
              h-[32px] px-[var(--spacing-md)]
              bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)] border-none
              text-[var(--text-sm)] font-medium cursor-pointer
              hover:bg-[var(--color-primary-hover)]
            "
          >
            프로젝트 목록으로
          </button>
        )}
      </div>
    </div>
  );
}
