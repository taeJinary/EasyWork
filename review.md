# 🔍 PR #164 코드 리뷰 — Phase B 프론트엔드

> **리뷰 대상:** `feat(frontend): Phase B - 프로젝트 보드(칸반), 태스크 상세 Drawer, 태스크 리스트`
> **브랜치:** `feature/frontend-phase-b` → `develop`
> **리뷰 일자:** 2026-03-06

---

## 요약

| 심각도 | 건수 |
|--------|------|
| 🚨 버그 (즉시 수정 필요) | 4건 |
| ⚠️ 잠재적 버그 | 4건 |
| 💡 개선 제안 | 4건 |

---

## 🚨 심각 (버그)

### [Bug 1] `TaskDetailDrawer` — `targetPosition` 불일치 가능성

**파일:** `frontend/src/components/TaskDetailDrawer.tsx`

```tsx
const handleStatusChange = async (newStatus: string) => {
  if (!task) return;
  try {
    await apiClient.patch(`/tasks/${taskId}/move`, {
      toStatus: newStatus,
      targetPosition: task.position,  // ❌ 기존 컬럼의 position 그대로 전송
      version: task.version,
    });
    setTask({ ...task, status: newStatus as TStatus, version: task.version + 1 });
```

**문제:**
`targetPosition`을 기존 `task.position` 그대로 전송합니다.
상태(컬럼)가 바뀌면 새 컬럼의 마지막 position으로 넣어야 하는데,
다른 컬럼의 position 값을 그대로 쓰면 서버에서 position 충돌이 발생할 수 있습니다.

**수정 방향:**
새 컬럼의 마지막 position + 1 혹은 서버 API 스펙에 맞춰 `targetPosition` 계산 로직 추가가 필요합니다.

---

### [Bug 2] `TaskDetailDrawer` — Edit / Delete 버튼 onClick 핸들러 누락 (Dead UI)

**파일:** `frontend/src/components/TaskDetailDrawer.tsx`

```tsx
<button className="...">
  <Edit size={14} />   {/* ❌ onClick 없음 */}
</button>
<button className="...">
  <Trash2 size={14} /> {/* ❌ onClick 없음 */}
</button>
```

**문제:**
`onClick` 핸들러가 전혀 없습니다. 버튼이 클릭되어도 아무 일도 일어나지 않습니다.
사용자 입장에서 기능이 있는 것처럼 보이는 Silent Dead UI 버그입니다.

**수정 방향:**
- `onClick` 핸들러를 구현하거나
- 미구현 상태라면 `disabled` + `title="준비 중"` 처리

---

### [Bug 3] `TaskDetailDrawer` — Backdrop과 Drawer의 z-index 동일 (클릭 투과 위험)

**파일:** `frontend/src/components/TaskDetailDrawer.tsx`

```tsx
{/* Backdrop */}
<div
  className="fixed inset-0 bg-black/20 z-50"  // ❌ z-50
  onClick={onClose}
/>

{/* Drawer */}
<div className="
  fixed top-0 right-0 h-full w-[680px] max-w-full
  z-50 overflow-y-auto  {/* ❌ z-50 동일 */}
  ...">
```

**문제:**
Backdrop과 Drawer 모두 `z-50`으로 동일합니다.
브라우저에 따라 이벤트 버블링이 Backdrop까지 전파되어
Drawer 내부를 클릭했을 때 `onClose`가 호출될 수 있습니다.

**수정 방향:**
```tsx
// Drawer의 z-index를 Backdrop보다 높게 설정
className="... z-[51] ..."
```

---

### [Bug 4] `ProjectBoardPage` — 보드 필터 select들이 state와 연결되지 않음 (비동작 UI)

**파일:** `frontend/src/pages/ProjectBoardPage.tsx`

```tsx
<select className="...">  {/* ❌ value, onChange 없음 */}
  <option value="">Assignee: All</option>
</select>
<select className="...">  {/* ❌ value, onChange 없음 */}
  <option value="">Label: All</option>
</select>
<select className="...">  {/* ❌ value, onChange 없음 */}
  <option value="">Priority: All</option>
</select>
```

**문제:**
Assignee / Label / Priority 필터 셀렉트에 `value`와 `onChange`가 없습니다.
선택해도 아무 필터링도 일어나지 않으며, `filterTasks()`도 `searchQuery`만 참조하고 있어
이 3개 필터는 완전히 Dead Code입니다.

**수정 방향:**
각 필터에 대한 state (`assigneeFilter`, `labelFilter`, `priorityFilter`)를 추가하고
`filterTasks()` 함수에 해당 필터 조건을 반영해야 합니다.

---

## ⚠️ 잠재적 버그

### [Warn 1] `TaskListPage` — `sortBy` / `direction` 변경 시 `page`가 리셋되지 않음

**파일:** `frontend/src/pages/TaskListPage.tsx`

```tsx
<select
  value={sortBy}
  onChange={(e) => setSortBy(e.target.value)}  // ❌ setPage(0) 누락
>
<...>
<select
  value={direction}
  onChange={(e) => setDirection(e.target.value)}  // ❌ setPage(0) 누락
>
```

**문제:**
`searchQuery`와 `statusFilter`는 변경 시 `setPage(0)`를 호출하지만,
`sortBy`와 `direction`은 그렇지 않습니다.
마지막 페이지에서 정렬 변경 시 빈 결과가 나올 수 있습니다.

**수정:**
```tsx
onChange={(e) => { setSortBy(e.target.value); setPage(0); }}
onChange={(e) => { setDirection(e.target.value); setPage(0); }}
```

---

### [Warn 2] `TaskListPage` — `refreshList()`가 클로저의 stale `page` 참조

**파일:** `frontend/src/pages/TaskListPage.tsx`

```tsx
const refreshList = async () => {
  try {
    const params = { page, size: 20, sortBy, direction }; // 클로저로 캡처된 값
    ...
  }
};
```

**문제:**
Drawer에서 상태를 변경한 뒤 `refreshList()`가 호출될 때
클로저로 캡처된 오래된 `page`, `sortBy`, `direction` 값을 사용합니다.
`statusFilter`로 필터링 중일 때 Drawer에서 다른 상태로 변경하면
해당 아이템이 현재 목록에서 사라지지 않는 불일치 현상이 발생합니다.

**수정 방향:**
`refreshList` 대신 `useEffect` 의존성을 trigger하는 상태값 변경 방식 사용 권장.

---

### [Warn 3] `formatTimeAgo` — 방금 생성된 항목이 "0분 전"으로 표시됨

**파일:** `frontend/src/components/TaskDetailDrawer.tsx`

```tsx
function formatTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}분 전`;  // ❌ "0분 전" 출력 가능
  ...
}
```

**수정:**
```tsx
if (minutes < 1) return '방금 전';
if (minutes < 60) return `${minutes}분 전`;
```

---

### [Warn 4] 에러 핸들링 전면 누락

**파일:** `TaskDetailDrawer.tsx`, `ProjectBoardPage.tsx`, `TaskListPage.tsx`

```tsx
} catch {
  // Error handling  ← 모든 catch 블록이 비어있음
}
```

**문제:**
API 실패 시 사용자에게 아무런 피드백이 없습니다.
네트워크 오류, 인증 만료, 서버 에러 등 모든 실패가 조용히 무시됩니다.

**수정 방향:**
- `errorMessage` state 추가 후 UI에 표시
- 또는 전역 toast/notification 시스템 연동

---

## 💡 개선 제안

### [Suggestion 1] `TaskDetailDrawer` — 댓글 작성 후 `commentCount` 미반영

댓글을 추가해도 `task.commentCount`가 로컬 state에서 갱신되지 않습니다.
헤더의 `Activity (N)` 카운트는 즉시 반영되지만, 카드의 댓글 수 배지는 갱신되지 않습니다.

```tsx
// handleCommentSubmit 성공 후
setTask(prev => prev ? { ...prev, commentCount: prev.commentCount + 1 } : null);
```

---

### [Suggestion 2] 헤더/탭/필터바 코드 중복

`ProjectBoardPage`와 `TaskListPage`에 헤더, 탭, FilterBar, "New Task" 버튼 코드가 거의 동일하게 중복되어 있습니다.

**제안:** 공통 `ProjectPageLayout` 컴포넌트를 추출하면 유지보수성이 크게 향상됩니다.

---

### [Suggestion 3] `BoardColumn` — `IN_PROGRESS` 아이콘 애니메이션 없음

`Loader` 아이콘은 정적으로 렌더링되어 진행 중임을 시각적으로 표현하지 못합니다.

```tsx
// IN_PROGRESS 컬럼 헤더
<Icon size={14} style={{ color: config.color }} className={status === 'IN_PROGRESS' ? 'animate-spin' : ''} />
```

---

### [Suggestion 4] `TaskDetailDrawer` — 첨부파일 다운로드 링크 없음

첨부파일 목록에 파일명과 크기만 표시되고 다운로드/미리보기 링크가 없습니다.
`Attachment` 타입에 `fileUrl` 혹은 presigned URL 필드 추가가 필요합니다.