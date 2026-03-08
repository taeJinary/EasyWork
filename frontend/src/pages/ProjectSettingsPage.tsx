import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Settings, Trash2, AlertCircle, X, CheckCircle2 } from 'lucide-react';
import apiClient from '@/api/client';
import { AxiosError } from 'axios';
import { toProjectDetail } from '@/utils/projectMappers';
import type { ApiResponse, ProjectDetailResponse } from '@/types';
import ErrorState from '@/components/ErrorState';
import PageHeader from '@/components/PageHeader';

interface UpdateProjectRequest {
  name: string;
  description: string;
}

interface ProjectDraftState {
  projectId: string;
  name: string | null;
  description: string | null;
}

interface DeleteDialogState {
  projectId: string;
  confirmName: string;
  error: string | null;
}

export default function ProjectSettingsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentProjectId = projectId ?? '';

  const [draft, setDraft] = useState<ProjectDraftState>({
    projectId: currentProjectId,
    name: null,
    description: null,
  });
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  
  // Danger Zone
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState<DeleteDialogState>({
    projectId: currentProjectId,
    confirmName: '',
    error: null,
  });

  const { data: projectRes, isLoading, error } = useQuery({
    queryKey: ['project', projectId],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<ProjectDetailResponse>>(`/projects/${projectId}`);
      return toProjectDetail(res.data.data);
    },
    enabled: !!projectId,
    retry: false, // Don't retry on 403 or 404
  });

  const project = projectRes;

  // Derive editable fields from server data (avoids setState-in-effect lint)
  const serverName = project?.name ?? '';
  const serverDesc = project?.description ?? '';
  const isCurrentDraft = draft.projectId === currentProjectId;
  const isCurrentDeleteDialog = deleteDialog.projectId === currentProjectId;
  const editableName = isCurrentDraft ? draft.name ?? serverName : serverName;
  const editableDesc = isCurrentDraft ? draft.description ?? serverDesc : serverDesc;
  const deleteConfirmName = isCurrentDeleteDialog ? deleteDialog.confirmName : '';
  const deleteError = isCurrentDeleteDialog ? deleteDialog.error : null;

  const updateDraft = (
    updater: (state: { name: string | null; description: string | null }) => {
      name: string | null;
      description: string | null;
    }
  ) => {
    setDraft((prev) => {
      const baseState =
        prev.projectId === currentProjectId
          ? { name: prev.name, description: prev.description }
          : { name: null, description: null };

      return {
        projectId: currentProjectId,
        ...updater(baseState),
      };
    });
  };

  const updateDeleteDialog = (
    updater: (state: { confirmName: string; error: string | null }) => {
      confirmName: string;
      error: string | null;
    }
  ) => {
    setDeleteDialog((prev) => {
      const baseState =
        prev.projectId === currentProjectId
          ? { confirmName: prev.confirmName, error: prev.error }
          : { confirmName: '', error: null };

      return {
        projectId: currentProjectId,
        ...updater(baseState),
      };
    });
  };

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: async (data: UpdateProjectRequest) => {
      await apiClient.patch(`/projects/${projectId}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setToastMessage('프로젝트 설정이 저장되었습니다.');
      setShowToast(true);
      setTimeout(() => setShowToast(false), 3000);
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: async () => {
      await apiClient.delete(`/projects/${projectId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate('/projects');
    },
    onError: () => {
      updateDeleteDialog((state) => ({
        ...state,
        error:
          '\uD504\uB85C\uC81D\uD2B8 \uC0AD\uC81C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. \uAD8C\uD55C\uC744 \uD655\uC778\uD558\uAC70\uB098 \uB2E4\uC2DC \uC2DC\uB3C4\uD574\uC8FC\uC138\uC694.',
      }));
    },
  });

  if (isLoading) {
    return (
      <div className="p-[var(--spacing-lg)] animate-pulse space-y-4">
        <div className="h-8 bg-[var(--color-surface-muted)] rounded w-1/4 mb-8" />
        <div className="h-32 bg-[var(--color-surface-muted)] rounded w-full max-w-2xl" />
      </div>
    );
  }

  // Handle Permission/Auth errors
  if (error) {
    const axiosErr = error as AxiosError;
    const isForbidden = axiosErr.response?.status === 403;
    const isNotFound = axiosErr.response?.status === 404;
    
    return (
      <ErrorState 
        title={isForbidden ? "접근 권한 없음" : isNotFound ? "프로젝트를 찾을 수 없음" : "오류 발생"}
        message={
          isForbidden 
            ? "이 프로젝트의 설정에 접근할 권한이 없습니다." 
            : isNotFound
              ? "요청하신 프로젝트가 존재하지 않거나 삭제되었습니다."
              : "프로젝트 정보를 불러오는데 실패했습니다."
        }
      />
    );
  }

  if (!project) return null;

  const isChanged = editableName !== serverName || editableDesc !== serverDesc;
  const isValid = editableName.trim().length >= 2 && editableName.trim().length <= 50 && editableDesc.length <= 500;
  
  // NOTE: Assuming project role/ownership dictates capability to edit or delete.
  // In a real app, you'd check `project.myRole === 'OWNER'` or similar, but we'll 
  // assume the API guards it, and we might just hide or show. For simplicity, we show it.

  const handleUpdate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid || updateMutation.isPending) return;
    updateMutation.mutate({ name: editableName.trim(), description: editableDesc.trim() });
  };

  const handleDelete = () => {
    if (deleteConfirmName !== project.name || deleteMutation.isPending) return;
    deleteMutation.mutate();
  };

  return (
    <>
      <div className="mb-[var(--spacing-lg)]">
        <div className="flex items-center gap-[var(--spacing-sm)] text-[var(--color-text-muted)] text-[var(--text-sm)] mb-[var(--spacing-sm)]">
          <span>Workspace</span>
          <span>/</span>
          <span className="font-semibold text-[var(--color-text-primary)]">{project.name}</span>
        </div>
        <PageHeader 
          title="Project Settings" 
          description="프로젝트의 기본 정보를 수정하거나 위험한 작업을 수행합니다."
        />
      </div>

      {showToast && (
        <div className="fixed top-[70px] right-[var(--spacing-lg)] z-50 animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-sm)] bg-[var(--color-accent-green)] text-[var(--color-success)] border border-[var(--color-success)] rounded-[var(--radius-sm)] shadow-[var(--shadow-sm)]">
            <CheckCircle2 size={16} />
            <span className="text-[var(--text-sm)] font-medium">{toastMessage}</span>
          </div>
        </div>
      )}

      <div className="max-w-[760px] space-y-[var(--spacing-xl)]">
        {/* General Settings */}
        <section>
          <div className="border-b border-[var(--color-border)] pb-[var(--spacing-sm)] mb-[var(--spacing-base)] flex items-center gap-[var(--spacing-xs)]">
            <Settings size={18} className="text-[var(--color-text-secondary)]" />
            <h2 className="text-[var(--text-md)] font-semibold text-[var(--color-text-primary)] m-0">
              General
            </h2>
          </div>

          <form onSubmit={handleUpdate} className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-[var(--radius-md)] p-[var(--spacing-lg)]">
            {updateMutation.isError && (
              <div className="mb-[var(--spacing-base)] p-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)] flex items-center gap-2">
                <AlertCircle size={14} />
                설정 저장에 실패했습니다.
              </div>
            )}

            <div className="mb-[var(--spacing-base)]">
              <label htmlFor="projectName" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                프로젝트 이름 (필수)
              </label>
              <input
                id="projectName"
                type="text"
                value={editableName}
                onChange={(e) =>
                  updateDraft((state) => ({
                    ...state,
                    name: e.target.value,
                  }))
                }
                maxLength={50}
                required
                className="
                  w-full max-w-[400px] h-[36px] px-[var(--spacing-sm)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
                "
              />
              <p className="text-[var(--text-xs)] text-[var(--color-text-muted)] mt-[var(--spacing-xs)] m-0">
                2자 이상 50자 이하로 입력해주세요.
              </p>
            </div>

            <div className="mb-[var(--spacing-lg)]">
              <label htmlFor="projectDesc" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                설명 (선택)
              </label>
              <textarea
                id="projectDesc"
                value={editableDesc}
                onChange={(e) =>
                  updateDraft((state) => ({
                    ...state,
                    description: e.target.value,
                  }))
                }
                maxLength={500}
                rows={4}
                className="
                  w-full p-[var(--spacing-sm)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
                  resize-vertical
                "
              />
              <div className="flex justify-between mt-[var(--spacing-xs)]">
                <p className="text-[var(--text-xs)] text-[var(--color-text-muted)] m-0">
                  프로젝트의 목적이나 내용을 간단히 설명해주세요.
                </p>
                <div className="text-[var(--text-xs)] text-[var(--color-text-muted)]">
                  {editableDesc.length}/500
                </div>
              </div>
            </div>

            <div className="pt-[var(--spacing-base)] border-t border-[var(--color-border)]">
              <button
                type="submit"
                disabled={!isChanged || !isValid || updateMutation.isPending}
                className="
                  h-[32px] px-[var(--spacing-lg)]
                  bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)] border-none
                  text-[var(--text-sm)] font-medium cursor-pointer
                  hover:bg-[var(--color-primary-hover)]
                  disabled:opacity-50 disabled:cursor-not-allowed
                "
              >
                {updateMutation.isPending ? '저장 중...' : '변경사항 저장'}
              </button>
            </div>
          </form>
        </section>

        {/* Danger Zone */}
        <section>
          <div className="border-b border-[var(--color-danger)] pb-[var(--spacing-sm)] mb-[var(--spacing-base)] flex items-center gap-[var(--spacing-xs)]">
            <Trash2 size={18} className="text-[var(--color-danger)]" />
            <h2 className="text-[var(--text-md)] font-semibold text-[var(--color-danger)] m-0">
              Danger Zone
            </h2>
          </div>

          <div className="bg-[var(--color-surface)] border border-[var(--color-danger)] rounded-[var(--radius-md)] p-[var(--spacing-lg)] flex items-center justify-between gap-[var(--spacing-lg)]">
            <div>
              <h3 className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)] m-0 mb-[var(--spacing-xs)]">
                Delete this project
              </h3>
              <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] m-0">
                프로젝트를 삭제하면 보드, 태스크, 댓글 등 모든 연관 데이터가 영구적으로 삭제되며 복구할 수 없습니다.
              </p>
            </div>
            <button
              onClick={() => {
                setShowDeleteModal(true);
                updateDeleteDialog(() => ({ confirmName: '', error: null }));
              }}
              className="
                h-[32px] px-[var(--spacing-md)] shrink-0
                bg-[var(--color-surface)] text-[var(--color-danger)] border border-[var(--color-danger)]
                rounded-[var(--radius-sm)] text-[var(--text-sm)] font-medium cursor-pointer
                hover:bg-[var(--color-accent-red)] transition-colors
              "
            >
              Delete project
            </button>
          </div>
        </section>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <>
          <div className="fixed inset-0 bg-black/50 z-40 transition-opacity" onClick={() => setShowDeleteModal(false)} />
          <div className="
            fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2
            w-[400px] max-w-[90vw] bg-[var(--color-surface)]
            border border-[var(--color-border)] rounded-[var(--radius-md)]
            shadow-lg z-50 p-[var(--spacing-lg)]
            animate-in fade-in zoom-in-95 duration-200
          ">
            <div className="flex items-center justify-between mb-[var(--spacing-base)]">
              <h3 className="text-[var(--text-base)] font-bold text-[var(--color-danger)] m-0 flex items-center gap-2">
                <AlertCircle size={18} />
                Are you absolutely sure?
              </h3>
              <button
                onClick={() => setShowDeleteModal(false)}
                className="p-[var(--spacing-xs)] bg-transparent border-none cursor-pointer text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)]"
              >
                <X size={16} />
              </button>
            </div>

            <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] leading-relaxed mb-[var(--spacing-base)] m-0">
              이 작업은 되돌릴 수 없습니다. 프로젝트의 모든 데이터가 즉시 삭제됩니다.
            </p>

            {deleteError && (
              <div className="mb-[var(--spacing-base)] p-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)] flex items-center gap-2">
                <AlertCircle size={14} />
                {deleteError}
              </div>
            )}
            
            <p className="text-[var(--text-sm)] text-[var(--color-text-primary)] mb-[var(--spacing-xs)] m-0">
              확인을 위해 프로젝트 이름 <span className="font-bold select-all bg-[var(--color-surface-muted)] px-1 rounded">{project.name}</span> 을(를) 입력해 주세요.
            </p>

            <div className="mb-[var(--spacing-lg)]">
              <input
                type="text"
                value={deleteConfirmName}
                onChange={(e) =>
                  updateDeleteDialog((state) => ({
                    ...state,
                    confirmName: e.target.value,
                  }))
                }
                className="
                  w-full h-[36px] px-[var(--spacing-sm)] mt-[var(--spacing-sm)]
                  border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)]
                  focus:outline-none focus:border-[var(--color-danger)] focus:ring-1 focus:ring-[var(--color-danger)]
                "
              />
            </div>

            <button
              onClick={handleDelete}
              disabled={deleteConfirmName !== project.name || deleteMutation.isPending}
              className="
                w-full h-[36px]
                bg-[var(--color-danger)] text-white rounded-[var(--radius-sm)]
                text-[var(--text-sm)] font-medium border-none cursor-pointer
                hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed
                flex items-center justify-center gap-2
              "
            >
              {deleteMutation.isPending ? '삭제 중...' : 'I understand the consequences, delete this project'}
            </button>
          </div>
        </>
      )}
    </>
  );
}
