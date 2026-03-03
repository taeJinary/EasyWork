package com.taskflow.backend.domain.project.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.taskflow.backend.domain.project.entity.QProject;
import com.taskflow.backend.domain.project.entity.QProjectMember;
import com.taskflow.backend.domain.task.entity.QTask;
import com.taskflow.backend.global.common.enums.ProjectRole;
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
public class ProjectQueryRepositoryImpl implements ProjectQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProjectListQueryResult> findMyProjects(Long userId, String keyword, ProjectRole role, Pageable pageable) {
        QProjectMember projectMember = QProjectMember.projectMember;
        QProject project = QProject.project;
        QProjectMember memberCountSource = new QProjectMember("memberCountSource");
        QTask taskCountSource = QTask.task;
        QTask doneTaskCountSource = new QTask("doneTaskCountSource");
        Expression<Long> memberCountExpr = JPAExpressions.select(memberCountSource.count())
                .from(memberCountSource)
                .where(memberCountSource.project.id.eq(project.id));
        Expression<Long> taskCountExpr = JPAExpressions.select(taskCountSource.count())
                .from(taskCountSource)
                .where(taskCountSource.project.id.eq(project.id)
                        .and(taskCountSource.deletedAt.isNull()));
        Expression<Long> doneTaskCountExpr = JPAExpressions.select(doneTaskCountSource.count())
                .from(doneTaskCountSource)
                .where(doneTaskCountSource.project.id.eq(project.id)
                        .and(doneTaskCountSource.deletedAt.isNull())
                        .and(doneTaskCountSource.status.eq(TaskStatus.DONE)));

        BooleanBuilder whereCondition = new BooleanBuilder()
                .and(projectMember.user.id.eq(userId))
                .and(project.deletedAt.isNull());

        if (role != null) {
            whereCondition.and(projectMember.role.eq(role));
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            whereCondition.and(
                    project.name.coalesce("").lower().contains(normalizedKeyword)
                            .or(project.description.coalesce("").lower().contains(normalizedKeyword))
            );
        }

        List<Tuple> rows = queryFactory
                .select(
                        project.id,
                        project.name,
                        project.description,
                        projectMember.role,
                        memberCountExpr,
                        taskCountExpr,
                        doneTaskCountExpr,
                        project.updatedAt
                )
                .from(projectMember)
                .join(projectMember.project, project)
                .where(whereCondition)
                .orderBy(project.updatedAt.desc(), project.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(projectMember.count())
                .from(projectMember)
                .join(projectMember.project, project)
                .where(whereCondition)
                .fetchOne();

        List<ProjectListQueryResult> content = rows.stream()
                .map(row -> new ProjectListQueryResult(
                        row.get(project.id),
                        row.get(project.name),
                        row.get(project.description),
                        row.get(projectMember.role),
                        row.get(memberCountExpr),
                        row.get(taskCountExpr),
                        row.get(doneTaskCountExpr),
                        row.get(project.updatedAt)
                ))
                .toList();

        return new PageImpl<>(content, pageable, totalCount == null ? 0L : totalCount);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }
}
