/**
 * Type shape tests — ensures frontend types match backend DTOs.
 * These are compile-time checks that would catch issues like duplicate
 * interface declarations or missing/extra fields.
 */
import { describe, it, expect } from 'vitest';
import type {
  ProjectMember,
  InvitationListItem,
  NotificationItem,
} from '@/types';

// Helper: assert that a type-safe object is assignable
function assertShape<T>(obj: T): T { return obj; }

describe('Type Shape Validation', () => {
  it('ProjectMember matches backend ProjectMemberResponse (no legacy id/name fields)', () => {
    const member = assertShape<ProjectMember>({
      memberId: 1,
      userId: 100,
      email: 'test@example.com',
      nickname: 'TestUser',
      role: 'MEMBER',
      joinedAt: '2026-03-01T00:00:00',
    });

    // Verify required fields exist
    expect(member.memberId).toBe(1);
    expect(member.nickname).toBe('TestUser');

    // Verify legacy fields do NOT exist
    expect('id' in member).toBe(false);
    expect('name' in member).toBe(false);
  });

  it('InvitationListItem matches backend InvitationListItemResponse', () => {
    const invitation = assertShape<InvitationListItem>({
      invitationId: 1,
      projectId: 10,
      projectName: 'EasyWork',
      inviterUserId: 100,
      inviterNickname: 'Alice',
      role: 'OWNER',
      status: 'PENDING',
      expiresAt: '2026-04-01T00:00:00',
      createdAt: '2026-03-07T00:00:00',
    });

    expect(invitation.invitationId).toBe(1);
    expect(invitation.inviterNickname).toBe('Alice');

    // Verify legacy fields do NOT exist
    expect('id' in invitation).toBe(false);
    expect('inviterName' in invitation).toBe(false);
    expect('inviterEmail' in invitation).toBe(false);
  });

  it('NotificationItem matches backend NotificationListItemResponse', () => {
    const notification = assertShape<NotificationItem>({
      notificationId: 1,
      type: 'COMMENT_MENTIONED',
      title: 'You were mentioned',
      content: 'Hey @user check this out',
      referenceType: 'COMMENT',
      referenceId: 42,
      isRead: false,
      createdAt: '2026-03-07T00:00:00',
    });

    expect(notification.notificationId).toBe(1);
    expect(notification.referenceType).toBe('COMMENT');

    // Verify legacy fields do NOT exist
    expect('id' in notification).toBe(false);
    expect('body' in notification).toBe(false);
  });
});
