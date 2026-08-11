USE task_app;

-- =========================================================
-- DEMO USERS
-- Passwords:
-- admin@local       -> admin123
-- owner@example.com -> demo123
-- member@example.com -> demo123
-- viewer@example.com -> demo123
-- =========================================================

INSERT INTO users
(id, email, password_hash, full_name, must_change_password, active)
VALUES
    (
        1,
        'admin@local',
        '$2a$10$39avdqGlIoAuozAT1brDuujeDxNpzTgEVdTIRgoDfGYkL/mrkFIE2',
        'System Administrator',
        0,
        1
    ),
    (
        2,
        'owner@example.com',
        '$2a$10$.jpuOTU3rwTc9gPAGcUPOO1fxG1wot7YK2loN6m5p0xBl5NNtQYP6',
        'John Smith',
        0,
        1
    ),
    (
        3,
        'member@example.com',
        '$2a$10$.jpuOTU3rwTc9gPAGcUPOO1fxG1wot7YK2loN6m5p0xBl5NNtQYP6',
        'Emma Johnson',
        0,
        1
    ),
    (
        4,
        'viewer@example.com',
        '$2a$10$.jpuOTU3rwTc9gPAGcUPOO1fxG1wot7YK2loN6m5p0xBl5NNtQYP6',
        'Michael Brown',
        0,
        1
    );

-- =========================================================
-- DEMO BOARD
-- =========================================================

INSERT INTO boards
(id, name, archived_at)
VALUES
    (1, 'Website Redesign', NULL);

-- =========================================================
-- BOARD MEMBERS
-- =========================================================

INSERT INTO board_members
(board_id, user_id, role)
VALUES
    (1, 2, 'OWNER'),
    (1, 3, 'MEMBER'),
    (1, 4, 'VIEWER');

-- =========================================================
-- KANBAN LISTS
-- =========================================================

INSERT INTO lists
(id, board_id, title, position)
VALUES
    (1, 1, 'To do', 1000.000000),
    (2, 1, 'Doing', 2000.000000),
    (3, 1, 'Done', 3000.000000);

-- =========================================================
-- DEMO TASKS
-- =========================================================

INSERT INTO cards
(
    id,
    board_id,
    list_id,
    title,
    description,
    due_at,
    created_by,
    assigned_to,
    position,
    archived_at,
    priority,
    version
)
VALUES
    (
        1,
        1,
        1,
        'Design landing page',
        'Prepare the new landing page layout and responsive structure.',
        '2026-09-01 17:00:00',
        2,
        3,
        1000.000000,
        NULL,
        3,
        0
    ),
    (
        2,
        1,
        1,
        'Create database schema',
        'Define the database model required by the application.',
        '2026-09-05 17:00:00',
        2,
        2,
        2000.000000,
        NULL,
        2,
        0
    ),
    (
        3,
        1,
        2,
        'Implement authentication',
        'Implement secure authentication and authorization.',
        '2026-08-25 17:00:00',
        2,
        3,
        1000.000000,
        NULL,
        5,
        0
    ),
    (
        4,
        1,
        2,
        'Develop REST API',
        'Implement the backend API required for project integrations.',
        '2026-09-10 17:00:00',
        2,
        2,
        2000.000000,
        NULL,
        4,
        0
    ),
    (
        5,
        1,
        3,
        'Configure project structure',
        'Initial project structure and development environment setup.',
        NULL,
        2,
        3,
        1000.000000,
        NULL,
        1,
        0
    );

-- =========================================================
-- LABELS
-- =========================================================

INSERT INTO card_labels
(id, board_id, name, color, created_by)
VALUES
    (1, 1, 'Backend', 'BLUE', 2),
    (2, 1, 'High Priority', 'RED', 2);

INSERT INTO card_label_assignments
(card_id, label_id, created_by)
VALUES
    (3, 2, 2),
    (4, 1, 2);

-- =========================================================
-- CHECKLIST
-- =========================================================

INSERT INTO card_checklist_items
(
    id,
    card_id,
    title,
    done,
    position,
    created_by,
    completed_by,
    completed_at
)
VALUES
    (
        1,
        3,
        'Configure Spring Security',
        1,
        1000,
        2,
        3,
        '2026-08-10 10:00:00'
    ),
    (
        2,
        3,
        'Add authorization rules',
        0,
        2000,
        2,
        NULL,
        NULL
    );

-- =========================================================
-- COMMENTS
-- =========================================================

INSERT INTO card_comments
(id, card_id, author_user_id, body)
VALUES
    (
        1,
        3,
        2,
        'Please verify the authorization rules before marking this task as completed.'
    ),
    (
        2,
        3,
        3,
        'Authentication is implemented. I am currently testing permissions.'
    );

-- =========================================================
-- ACTIVITY HISTORY
-- =========================================================

INSERT INTO card_activity
(
    id,
    card_id,
    actor_user_id,
    actor_email,
    action,
    old_value,
    new_value
)
VALUES
    (
        1,
        3,
        2,
        'owner@example.com',
        'CREATED',
        NULL,
        'Task: Implement authentication'
    ),
    (
        2,
        3,
        2,
        'owner@example.com',
        'ASSIGNED',
        NULL,
        'member@example.com'
    ),
    (
        3,
        3,
        3,
        'member@example.com',
        'COMMENTED',
        NULL,
        'Authentication is implemented. I am currently testing permissions.'
    );

-- =========================================================
-- DEMO NOTIFICATION
-- =========================================================

INSERT INTO notifications
(
    id,
    user_id,
    type,
    title,
    message,
    board_id,
    card_id,
    created_by,
    is_read
)
VALUES
    (
        1,
        3,
        'TASK_ASSIGNED',
        'New task assigned',
        'You have been assigned to Implement authentication.',
        1,
        3,
        2,
        b'0'
    );