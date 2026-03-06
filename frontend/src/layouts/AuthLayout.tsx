import { Outlet, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

export default function AuthLayout() {
  const { isAuthenticated } = useAuthStore();

  if (isAuthenticated) {
    return <Navigate to="/workspaces" replace />;
  }

  return (
    <div className="
      min-h-screen bg-[var(--color-bg)]
      flex items-center justify-center p-[var(--spacing-lg)]
    ">
      <Outlet />
    </div>
  );
}
