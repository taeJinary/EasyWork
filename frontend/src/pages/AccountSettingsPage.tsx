import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock, AlertTriangle, AlertCircle, CheckCircle, X } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import apiClient from '@/api/client';
import { useAuthStore } from '@/stores/authStore';
import type {
  ApiResponse,
  NotificationPushTokenResponse,
  NotificationPushTokenUnregisterResponse,
  PushPlatform,
  PushTokenRegistrationState,
} from '@/types';

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/;

function validateNewPassword(value: string): string | null {
  if (value.length < 8) return '비밀번호는 8자 이상이어야 합니다.';
  if (value.length > 20) return '비밀번호는 20자 이하여야 합니다.';
  if (!PASSWORD_REGEX.test(value)) return '비밀번호는 영문, 숫자, 특수문자(@$!%*#?&)를 모두 포함해야 합니다.';
  return null;
}

export default function AccountSettingsPage() {
  const navigate = useNavigate();
  const { logout } = useAuthStore();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [pwSubmitting, setPwSubmitting] = useState(false);
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwSuccess, setPwSuccess] = useState<string | null>(null);
  const [pwValidation, setPwValidation] = useState<string | null>(null);

  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [withdrawPassword, setWithdrawPassword] = useState('');
  const [withdrawSubmitting, setWithdrawSubmitting] = useState(false);
  const [withdrawError, setWithdrawError] = useState<string | null>(null);

  // Push token
  const [pushToken, setPushToken] = useState('');
  const [pushPlatform, setPushPlatform] = useState<PushPlatform>('WEB');
  const [pushSubmitting, setPushSubmitting] = useState(false);
  const [pushError, setPushError] = useState<string | null>(null);
  const [pushSuccess, setPushSuccess] = useState<string | null>(null);
  const [registeredDevices, setRegisteredDevices] = useState<PushTokenRegistrationState[]>([]);
  const [pushDevicesLoading, setPushDevicesLoading] = useState(true);
  const [pushDevicesLoadError, setPushDevicesLoadError] = useState<string | null>(null);

  const handleNewPasswordChange = (value: string) => {
    setNewPassword(value);
    setPwSuccess(null);
    const err = value.trim() ? validateNewPassword(value) : null;
    setPwValidation(err);
  };

  const canChangePassword = currentPassword.trim().length > 0 && newPassword.trim().length > 0 && !pwValidation && !pwSubmitting;
  const canRegisterDevice = pushToken.trim().length > 0 && !pushSubmitting;

  const loadRegisteredDevices = async () => {
    setPushDevicesLoading(true);
    setPushDevicesLoadError(null);
    try {
      const response = await apiClient.get<ApiResponse<NotificationPushTokenResponse[]>>('/notifications/push-tokens');
      setRegisteredDevices(response.data.data.map((device) => ({
        token: device.token,
        platform: device.platform,
        active: device.active,
      })));
    } catch (err) {
      setRegisteredDevices([]);
      setPushDevicesLoadError('등록된 디바이스 목록을 불러오지 못했습니다.');
      console.error('Failed to load push tokens:', err);
    } finally {
      setPushDevicesLoading(false);
    }
  };

  useEffect(() => {
    void loadRegisteredDevices();
  }, []);

  const handleChangePassword = async () => {
    const err = validateNewPassword(newPassword);
    if (err) {
      setPwValidation(err);
      return;
    }
    setPwSubmitting(true);
    setPwError(null);
    setPwSuccess(null);
    try {
      await apiClient.patch<ApiResponse<void>>('/users/me/password', {
        currentPassword,
        newPassword,
      });
      setPwSuccess('비밀번호가 변경되었습니다.');
      setCurrentPassword('');
      setNewPassword('');
      setPwValidation(null);
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setPwError(message || '비밀번호 변경에 실패했습니다.');
      console.error('Failed to change password:', err);
    } finally {
      setPwSubmitting(false);
    }
  };

  const handleWithdraw = async () => {
    setWithdrawSubmitting(true);
    setWithdrawError(null);
    try {
      await apiClient.delete<ApiResponse<void>>('/users/me', {
        data: { password: withdrawPassword },
      });
      logout();
      navigate('/login', { replace: true });
    } catch (err) {
      setWithdrawError('탈퇴에 실패했습니다. 비밀번호를 확인해주세요.');
      console.error('Failed to withdraw:', err);
    } finally {
      setWithdrawSubmitting(false);
    }
  };

  const handleRegisterPushToken = async () => {
    const normalizedToken = pushToken.trim();
    if (!normalizedToken) {
      setPushError('푸시 토큰을 입력해주세요.');
      return;
    }

    setPushSubmitting(true);
    setPushError(null);
    setPushSuccess(null);

    try {
      const response = await apiClient.post<ApiResponse<NotificationPushTokenResponse>>(
        '/notifications/push-tokens',
        {
          token: normalizedToken,
          platform: pushPlatform,
        }
      );

      setRegisteredDevices((current) => {
        const next = current.filter((device) => device.token !== response.data.data.token);
        return [
          {
            token: response.data.data.token,
            platform: response.data.data.platform,
            active: response.data.data.active,
          },
          ...next,
        ];
      });
      setPushToken('');
      setPushSuccess('활성 디바이스가 등록되었습니다.');
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setPushError(message || '디바이스 등록에 실패했습니다.');
      console.error('Failed to register push token:', err);
    } finally {
      setPushSubmitting(false);
    }
  };

  const handleUnregisterPushToken = async (token: string) => {
    setPushSubmitting(true);
    setPushError(null);
    setPushSuccess(null);

    try {
      const response = await apiClient.delete<ApiResponse<NotificationPushTokenUnregisterResponse>>(
        '/notifications/push-tokens',
        {
          params: { token },
        }
      );

      if (response.data.data.removed) {
        setRegisteredDevices((current) => current.filter((device) => device.token !== token));
        setPushSuccess('디바이스 등록이 해제되었습니다.');
      }
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setPushError(message || '디바이스 해제에 실패했습니다.');
      console.error('Failed to unregister push token:', err);
    } finally {
      setPushSubmitting(false);
    }
  };

  return (
    <div>
      <PageHeader title="계정 설정" description="비밀번호 변경 및 계정 관리" />

      <div className="mt-[var(--spacing-lg)] max-w-[480px] space-y-[var(--spacing-xl)]">
        <section>
          <h2 className="text-[var(--text-base)] font-bold text-[var(--color-text-primary)] m-0 mb-[var(--spacing-base)] flex items-center gap-[var(--spacing-sm)]">
            <Lock size={16} />
            비밀번호 변경
          </h2>

          {pwError && (
            <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mb-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
              <AlertCircle size={14} className="shrink-0" />
              {pwError}
            </div>
          )}
          {pwSuccess && (
            <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mb-[var(--spacing-sm)] bg-green-50 border border-[var(--color-success)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-success)]">
              <CheckCircle size={14} className="shrink-0" />
              {pwSuccess}
            </div>
          )}

          <div className="space-y-[var(--spacing-md)]">
            <div>
              <label htmlFor="currentPassword" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                현재 비밀번호
              </label>
              <input
                id="currentPassword"
                type="password"
                value={currentPassword}
                onChange={(e) => { setCurrentPassword(e.target.value); setPwSuccess(null); }}
                className="
                  w-full h-[36px] px-[var(--spacing-sm)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
                "
              />
            </div>
            <div>
              <label htmlFor="newPassword" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                새 비밀번호
              </label>
              <input
                id="newPassword"
                type="password"
                value={newPassword}
                onChange={(e) => handleNewPasswordChange(e.target.value)}
                className={`
                  w-full h-[36px] px-[var(--spacing-sm)]
                  border rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:ring-1
                  ${pwValidation
                    ? 'border-[var(--color-danger)] focus:border-[var(--color-danger)] focus:ring-[var(--color-danger)]'
                    : 'border-[var(--color-border)] focus:border-[var(--color-primary)] focus:ring-[var(--color-primary)]'
                  }
                `}
              />
              {pwValidation && (
                <p className="text-[var(--text-xs)] text-[var(--color-danger)] mt-[var(--spacing-xs)] m-0">
                  {pwValidation}
                </p>
              )}
              <p className="text-[var(--text-xs)] text-[var(--color-text-muted)] mt-[var(--spacing-xs)] m-0">
                8~20자, 영문 + 숫자 + 특수문자 포함
              </p>
            </div>
            <button
              onClick={handleChangePassword}
              disabled={!canChangePassword}
              className="
                h-[36px] px-[var(--spacing-lg)]
                bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
                text-[var(--text-sm)] font-medium border-none cursor-pointer
                hover:bg-[var(--color-primary-hover)]
                disabled:opacity-50 disabled:cursor-not-allowed
              "
            >
              {pwSubmitting ? '변경 중...' : '비밀번호 변경'}
            </button>
          </div>
        </section>

        <section className="border-t border-[var(--color-border)] pt-[var(--spacing-lg)]">
          <h2 className="text-[var(--text-base)] font-bold text-[var(--color-text-primary)] m-0 mb-[var(--spacing-base)]">
            알림 디바이스
          </h2>

          {pushError && (
            <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mb-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
              <AlertCircle size={14} className="shrink-0" />
              {pushError}
            </div>
          )}
          {pushSuccess && (
            <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mb-[var(--spacing-sm)] bg-green-50 border border-[var(--color-success)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-success)]">
              <CheckCircle size={14} className="shrink-0" />
              {pushSuccess}
            </div>
          )}

          <div className="space-y-[var(--spacing-md)]">
            <div>
              <label htmlFor="pushToken" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                푸시 토큰
              </label>
              <input
                id="pushToken"
                type="text"
                value={pushToken}
                onChange={(e) => {
                  setPushToken(e.target.value);
                  setPushError(null);
                  setPushSuccess(null);
                }}
                placeholder="device token"
                className="
                  w-full h-[36px] px-[var(--spacing-sm)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
                "
              />
            </div>

            <div>
              <label htmlFor="pushPlatform" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                플랫폼
              </label>
              <select
                id="pushPlatform"
                value={pushPlatform}
                onChange={(e) => {
                  setPushPlatform(e.target.value as PushPlatform);
                  setPushError(null);
                  setPushSuccess(null);
                }}
                className="
                  w-full h-[36px] px-[var(--spacing-sm)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
                "
              >
                <option value="WEB">WEB</option>
                <option value="ANDROID">ANDROID</option>
                <option value="IOS">IOS</option>
              </select>
            </div>

            <div className="flex gap-[var(--spacing-sm)]">
              <button
                onClick={handleRegisterPushToken}
                disabled={!canRegisterDevice}
                className="
                  h-[36px] px-[var(--spacing-lg)]
                  bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
                  text-[var(--text-sm)] font-medium border-none cursor-pointer
                  hover:bg-[var(--color-primary-hover)]
                  disabled:opacity-50 disabled:cursor-not-allowed
                "
              >
                {pushSubmitting ? '처리 중...' : '디바이스 등록'}
              </button>
            </div>
          </div>

          {pushDevicesLoadError && (
            <div className="mt-[var(--spacing-lg)] rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-base)]">
              <div className="flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                <AlertCircle size={14} className="shrink-0" />
                {pushDevicesLoadError}
              </div>
              <button
                onClick={() => void loadRegisteredDevices()}
                className="
                  mt-[var(--spacing-sm)] h-[32px] px-[var(--spacing-md)]
                  border border-[var(--color-danger)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-danger)]
                  cursor-pointer hover:bg-[var(--color-surface-muted)]
                "
              >
                다시 시도
              </button>
            </div>
          )}

          {!pushDevicesLoadError && !pushDevicesLoading && registeredDevices.length === 0 && (
            <div className="mt-[var(--spacing-lg)] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-[var(--spacing-base)]">
              <h3 className="m-0 mb-[var(--spacing-xs)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                등록된 디바이스가 없습니다.
              </h3>
              <p className="m-0 text-[var(--text-sm)] text-[var(--color-text-secondary)]">
                새 토큰을 등록하면 이 기기에서 알림을 받을 수 있습니다.
              </p>
            </div>
          )}

          {!pushDevicesLoadError && registeredDevices.length > 0 && (
            <div
              data-testid="registered-device-list"
              className="mt-[var(--spacing-lg)] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-[var(--spacing-base)]"
            >
              <h3 className="m-0 mb-[var(--spacing-sm)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                등록된 디바이스
              </h3>
              <div className="space-y-[var(--spacing-sm)]">
                {registeredDevices.map((device) => (
                  <div
                    key={device.token}
                    className="flex items-start justify-between gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-sm)]"
                  >
                    <div className="space-y-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
                      <p className="m-0">{device.platform}</p>
                      <p className="m-0 break-all">{device.token}</p>
                    </div>
                    <button
                      onClick={() => handleUnregisterPushToken(device.token)}
                      disabled={pushSubmitting}
                      aria-label={`디바이스 해제 ${device.token}`}
                      className="
                        h-[32px] shrink-0 px-[var(--spacing-md)]
                        bg-transparent border border-[var(--color-border)] text-[var(--color-text-secondary)]
                        rounded-[var(--radius-sm)] text-[var(--text-sm)] font-medium cursor-pointer
                        hover:bg-[var(--color-surface-muted)]
                        disabled:opacity-50 disabled:cursor-not-allowed
                      "
                    >
                      디바이스 해제
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>

        {/* ── Danger Zone ── */}
         <section className="border-t border-[var(--color-border)] pt-[var(--spacing-lg)]">
          <h2 className="text-[var(--text-base)] font-bold text-[var(--color-danger)] m-0 mb-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)]">
            <AlertTriangle size={16} />
            위험 영역
          </h2>
          <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mb-[var(--spacing-base)] m-0">
            계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다. 이 작업은 되돌릴 수 없습니다.
          </p>
          <button
            onClick={() => setShowWithdrawModal(true)}
            className="
              h-[36px] px-[var(--spacing-lg)]
              bg-transparent border border-[var(--color-danger)] text-[var(--color-danger)]
              rounded-[var(--radius-sm)] text-[var(--text-sm)] font-medium cursor-pointer
              hover:bg-[var(--color-accent-red)]
            "
          >
            회원 탈퇴
          </button>
        </section>
      </div>

      {showWithdrawModal && (
        <>
          <div className="fixed inset-0 bg-black/30 z-40" onClick={() => { setShowWithdrawModal(false); setWithdrawError(null); setWithdrawPassword(''); }} />
          <div className="
            fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2
            w-[400px] max-w-[90vw] bg-[var(--color-surface)]
            border border-[var(--color-border)] rounded-[var(--radius-md)]
            shadow-lg z-50 p-[var(--spacing-lg)]
          ">
            <div className="flex items-center justify-between mb-[var(--spacing-base)]">
              <h3 className="text-[var(--text-base)] font-bold text-[var(--color-danger)] m-0">
                정말 탈퇴하시겠습니까?
              </h3>
              <button
                onClick={() => { setShowWithdrawModal(false); setWithdrawError(null); setWithdrawPassword(''); }}
                className="p-[var(--spacing-xs)] bg-transparent border-none cursor-pointer text-[var(--color-text-muted)]"
              >
                <X size={16} />
              </button>
            </div>

            <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mb-[var(--spacing-base)] m-0">
              탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.
            </p>

            {withdrawError && (
              <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mb-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                <AlertCircle size={14} />
                {withdrawError}
              </div>
            )}

            <div className="mb-[var(--spacing-base)]">
              <label htmlFor="withdrawPassword" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                비밀번호 확인
              </label>
              <input
                id="withdrawPassword"
                type="password"
                value={withdrawPassword}
                onChange={(e) => setWithdrawPassword(e.target.value)}
                placeholder="비밀번호를 입력하세요"
                className="
                  w-full h-[36px] px-[var(--spacing-sm)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:border-[var(--color-danger)] focus:ring-1 focus:ring-[var(--color-danger)]
                "
              />
            </div>

            <div className="flex justify-end gap-[var(--spacing-sm)]">
              <button
                onClick={() => { setShowWithdrawModal(false); setWithdrawError(null); setWithdrawPassword(''); }}
                className="
                  h-[32px] px-[var(--spacing-md)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
                  cursor-pointer hover:bg-[var(--color-surface-muted)]
                "
              >
                취소
              </button>
              <button
                onClick={handleWithdraw}
                disabled={!withdrawPassword.trim() || withdrawSubmitting}
                className="
                  h-[32px] px-[var(--spacing-md)]
                  bg-[var(--color-danger)] text-white rounded-[var(--radius-sm)]
                  text-[var(--text-sm)] font-medium border-none cursor-pointer
                  hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed
                "
              >
                {withdrawSubmitting ? '처리 중...' : '탈퇴 확인'}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
