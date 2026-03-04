package com.taskflow.backend.domain.task.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.taskflow.backend.domain.task.entity.QTask;
import com.taskflow.backend.domain.task.entity.QTaskLabel;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class TaskQueryRepositoryImpl implements TaskQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Task> findTasks(
            Long projectId,
            TaskStatus status,
            String sortBy,
            String direction,
            String keyword,
            Pageable pageable
    ) {
        QTask task = QTask.task;
        String normalizedDirection = normalizeDirection(direction);
        boolean ascending = "ASC".equalsIgnoreCase(normalizedDirection);

        BooleanBuilder where = new BooleanBuilder()
                .and(task.project.id.eq(projectId))
                .and(task.deletedAt.isNull());

        if (status != null) {
            where.and(task.status.eq(status));
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            where.and(
                    task.title.coalesce("").lower().contains(normalizedKeyword)
                            .or(task.description.coalesce("").lower().contains(normalizedKeyword))
            );
        }

        Long totalCount = queryFactory
                .select(task.count())
                .from(task)
                .where(where)
                .fetchOne();

        if (pageable.getOffset() > Integer.MAX_VALUE) {
            return new PageImpl<>(List.of(), pageable, totalCount == null ? 0L : totalCount);
        }

        List<Task> content = queryFactory
                .selectFrom(task)
                .where(where)
                .orderBy(resolvePrimarySort(task, sortBy, ascending), resolveSecondarySort(task, ascending))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, totalCount == null ? 0L : totalCount);
    }

    @Override
    public List<Task> findTaskBoardTasks(
            Long projectId,
            Long assigneeUserId,
            TaskPriority priority,
            Long labelId,
            String keyword
    ) {
        QTask task = QTask.task;
        QTaskLabel taskLabel = QTaskLabel.taskLabel;

        BooleanBuilder where = new BooleanBuilder()
                .and(task.project.id.eq(projectId))
                .and(task.deletedAt.isNull());

        if (assigneeUserId != null) {
            where.and(task.assignee.id.eq(assigneeUserId));
        }

        if (priority != null) {
            where.and(task.priority.eq(priority));
        }

        if (labelId != null) {
            where.and(
                    JPAExpressions.selectOne()
                            .from(taskLabel)
                            .where(
                                    taskLabel.task.eq(task),
                                    taskLabel.label.id.eq(labelId)
                            )
                            .exists()
            );
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            where.and(
                    task.title.coalesce("").lower().contains(normalizedKeyword)
                            .or(task.description.coalesce("").lower().contains(normalizedKeyword))
            );
        }

        return queryFactory
                .selectFrom(task)
                .where(where)
                .orderBy(task.status.asc(), task.position.asc(), task.id.asc())
                .fetch();
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            return "DESC";
        }
        return direction.trim();
    }

    private OrderSpecifier<?> resolvePrimarySort(QTask task, String sortBy, boolean ascending) {
        String normalizedSortBy = sortBy == null ? "updatedAt" : sortBy.trim();

        return switch (normalizedSortBy) {
            case "createdAt" -> ascending ? task.createdAt.asc().nullsLast() : task.createdAt.desc().nullsLast();
            case "dueDate" -> ascending ? task.dueDate.asc().nullsLast() : task.dueDate.desc().nullsLast();
            case "priority" -> {
                NumberExpression<Integer> priorityRank = buildPriorityRank(task);
                yield ascending ? priorityRank.asc() : priorityRank.desc();
            }
            case "updatedAt" -> ascending ? task.updatedAt.asc().nullsLast() : task.updatedAt.desc().nullsLast();
            default -> ascending ? task.updatedAt.asc().nullsLast() : task.updatedAt.desc().nullsLast();
        };
    }

    private NumberExpression<Integer> buildPriorityRank(QTask task) {
        return new CaseBuilder()
                .when(task.priority.eq(TaskPriority.LOW)).then(0)
                .when(task.priority.eq(TaskPriority.MEDIUM)).then(1)
                .when(task.priority.eq(TaskPriority.HIGH)).then(2)
                .when(task.priority.eq(TaskPriority.URGENT)).then(3)
                .otherwise(99);
    }

    private OrderSpecifier<?> resolveSecondarySort(QTask task, boolean ascending) {
        return ascending ? task.id.asc() : task.id.desc();
    }
}
