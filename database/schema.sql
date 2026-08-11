-- Web Tasks - Database Schema
-- Generated from the current task_app schema export.
-- Contains schema only; no personal, test, or runtime data.

CREATE DATABASE IF NOT EXISTS `task_app`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `task_app`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `card_label_assignments`;
DROP TABLE IF EXISTS `card_labels`;
DROP TABLE IF EXISTS `card_comments`;
DROP TABLE IF EXISTS `card_checklist_items`;
DROP TABLE IF EXISTS `card_attachments`;
DROP TABLE IF EXISTS `card_activity`;
DROP TABLE IF EXISTS `board_members`;
DROP TABLE IF EXISTS `cards`;
DROP TABLE IF EXISTS `lists`;
DROP TABLE IF EXISTS `boards`;
DROP TABLE IF EXISTS `users`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------
-- Table: `users`
-- -----------------------------------------------------
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `must_change_password` tinyint(1) NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_active_full_name` (`active`,`full_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `boards`
-- -----------------------------------------------------
CREATE TABLE `boards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_boards_archived_at` (`archived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `lists`
-- -----------------------------------------------------
CREATE TABLE `lists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` decimal(18,6) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lists_board_pos` (`board_id`,`position`),
  CONSTRAINT `fk_list_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `cards`
-- -----------------------------------------------------
CREATE TABLE `cards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `list_id` bigint NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `due_at` datetime DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `assigned_to` bigint DEFAULT NULL,
  `position` decimal(18,6) NOT NULL,
  `archived_at` datetime DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `priority` tinyint NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_card_created_by` (`created_by`),
  KEY `idx_cards_list_pos` (`list_id`,`position`),
  KEY `idx_cards_board` (`board_id`),
  KEY `idx_cards_list_arch_pos` (`list_id`,`archived_at`,`position`),
  KEY `idx_cards_assigned_arch_upd` (`assigned_to`,`archived_at`,`updated_at` DESC),
  KEY `idx_cards_board_arch_list` (`board_id`,`archived_at`,`list_id`),
  KEY `idx_cards_board_archived_id` (`board_id`,`archived_at` DESC,`id` DESC),
  CONSTRAINT `fk_card_assigned_to` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_card_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`),
  CONSTRAINT `fk_card_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_card_list` FOREIGN KEY (`list_id`) REFERENCES `lists` (`id`),
  CONSTRAINT `chk_cards_priority` CHECK ((`priority` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `board_members`
-- -----------------------------------------------------
CREATE TABLE `board_members` (
  `board_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `joined_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`board_id`,`user_id`),
  KEY `idx_bm_user` (`user_id`),
  KEY `idx_bm_user_board` (`user_id`,`board_id`),
  KEY `idx_bm_user_role_board` (`user_id`,`role`,`board_id`),
  CONSTRAINT `fk_bm_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`),
  CONSTRAINT `fk_bm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `card_activity`
-- -----------------------------------------------------
CREATE TABLE `card_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_id` bigint NOT NULL,
  `actor_user_id` bigint NOT NULL,
  `actor_email` varchar(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_value` text COLLATE utf8mb4_unicode_ci,
  `new_value` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ca_card_created` (`card_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `card_attachments`
-- -----------------------------------------------------
CREATE TABLE `card_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_id` bigint NOT NULL,
  `uploaded_by` bigint NOT NULL,
  `original_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size_bytes` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ca_card_created` (`card_id`,`created_at`),
  KEY `idx_ca_uploaded_by` (`uploaded_by`),
  CONSTRAINT `fk_cat_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cat_uploaded_by` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `card_checklist_items`
-- -----------------------------------------------------
CREATE TABLE `card_checklist_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_id` bigint NOT NULL,
  `title` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `done` tinyint(1) NOT NULL DEFAULT '0',
  `position` int NOT NULL DEFAULT '1000',
  `created_by` bigint NOT NULL,
  `completed_by` bigint DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_card_checklist_card_pos` (`card_id`,`position`,`id`),
  KEY `idx_card_checklist_done` (`card_id`,`done`),
  KEY `fk_card_checklist_created_by` (`created_by`),
  KEY `fk_card_checklist_completed_by` (`completed_by`),
  CONSTRAINT `fk_card_checklist_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_card_checklist_completed_by` FOREIGN KEY (`completed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_card_checklist_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `card_comments`
-- -----------------------------------------------------
CREATE TABLE `card_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_id` bigint NOT NULL,
  `author_user_id` bigint NOT NULL,
  `body` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cc_card_time` (`card_id`,`created_at`),
  KEY `fk_cc_author` (`author_user_id`),
  CONSTRAINT `fk_cc_author` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_cc_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `card_labels`
-- -----------------------------------------------------
CREATE TABLE `card_labels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `color` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BLUE',
  `created_by` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_card_labels_board_name` (`board_id`,`name`),
  KEY `fk_card_labels_created_by` (`created_by`),
  KEY `idx_card_labels_board_id` (`board_id`),
  CONSTRAINT `fk_card_labels_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`),
  CONSTRAINT `fk_card_labels_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `card_label_assignments`
-- -----------------------------------------------------
CREATE TABLE `card_label_assignments` (
  `card_id` bigint NOT NULL,
  `label_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`card_id`,`label_id`),
  KEY `fk_card_label_assignments_created_by` (`created_by`),
  KEY `idx_card_label_assignments_label_id` (`label_id`),
  KEY `idx_card_label_assignments_card_id` (`card_id`),
  CONSTRAINT `fk_card_label_assignments_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`),
  CONSTRAINT `fk_card_label_assignments_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_card_label_assignments_label` FOREIGN KEY (`label_id`) REFERENCES `card_labels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table: `notifications`
-- -----------------------------------------------------
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci,
  `board_id` bigint DEFAULT NULL,
  `card_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `is_read` bit(1) NOT NULL DEFAULT b'0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_notifications_created_by` (`created_by`),
  KEY `ix_notifications_user_read_created` (`user_id`,`is_read`,`created_at` DESC),
  KEY `ix_notifications_user_created` (`user_id`,`created_at` DESC),
  KEY `ix_notifications_card` (`card_id`),
  CONSTRAINT `fk_notifications_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`),
  CONSTRAINT `fk_notifications_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- End of schema.
