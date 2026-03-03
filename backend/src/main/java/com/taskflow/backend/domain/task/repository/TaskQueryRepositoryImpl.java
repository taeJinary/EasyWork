package com.taskflow.backend.domain.task.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.taskflow.backend.domain.task.entity.QTask;
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
        boolean ascending = "ASC".equalsIgnoreCase(direction);

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

        List<Task> content = queryFactory
                .selectFrom(task)
                .where(where)
                .orderBy(resolvePrimarySort(task, sortBy, ascending), resolveSecondarySort(task, ascending))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(task.count())
                .from(task)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount == null ? 0L : totalCount);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private OrderSpecifier<?> resolvePrimarySort(QTask task, String sortBy, boolean ascending) {
        String normalizedSortBy = sortBy == null ? "updatedAt" : sortBy.trim();

        return switch (normalizedSortBy) {
            case "createdAt" -> ascending ? task.createdAt.asc().nullsLast() : task.createdAt.desc().nullsLast();
            case "dueDate" -> ascending ? task.dueDate.asc().nullsLast() : task.dueDate.desc().nullsLast();
            case "priority" -> ascending ? priorityOrder(task).asc() : priorityOrder(task).desc();
            case "updatedAt" -> ascending ? task.updatedAt.asc().nullsLast() : task.updatedAt.desc().nullsLast();
            default -> ascending ? task.updatedAt.asc().nullsLast() : task.updatedAt.desc().nullsLast();
        };
    }

    private OrderSpecifier<?> resolveSecondarySort(QTask task, boolean ascending) {
        return ascending ? task.id.asc() : task.id.desc();
    }

    private NumberExpression<Integer> priorityOrder(QTask task) {
        return new CaseBuilder()
                .when(task.priority.eq(TaskPriority.LOW)).then(0)
                .when(task.priority.eq(TaskPriority.MEDIUM)).then(1)
                .when(task.priority.eq(TaskPriority.HIGH)).then(2)
                .when(task.priority.eq(TaskPriority.URGENT)).then(3)
                .otherwise(Integer.MAX_VALUE);
    }
}
