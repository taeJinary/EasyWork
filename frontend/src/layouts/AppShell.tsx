import { Outlet, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import GlobalHeader from '@/components/GlobalHeader';
import Sidebar from '@/components/Sidebar';

export default function AppShell() {
  const { isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen bg-[var(--color-bg)]">
      <GlobalHeader />
      <Sidebar />
      <main className="md:ml-[256px] mt-[56px] p-[var(--spacing-md)] md:p-[var(--spacing-lg)] min-w-0">
        <Outlet />
      </main>
    </div>
  );
}
