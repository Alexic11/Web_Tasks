-- MySQL dump 10.13  Distrib 8.0.22, for Win64 (x86_64)
--
-- Host: localhost    Database: task_app
-- ------------------------------------------------------
-- Server version	8.0.21

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `board_members`
--

DROP TABLE IF EXISTS `board_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_members` (
  `board_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `joined_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`board_id`,`user_id`),
  KEY `idx_bm_user` (`user_id`),
  CONSTRAINT `fk_bm_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`),
  CONSTRAINT `fk_bm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `board_members`
--

LOCK TABLES `board_members` WRITE;
/*!40000 ALTER TABLE `board_members` DISABLE KEYS */;
INSERT INTO `board_members` VALUES (1,1,'OWNER','2026-02-25 07:42:02'),(1,3,'MEMBER','2026-02-26 08:11:06'),(5,2,'MEMBER','2026-02-26 11:10:15'),(5,3,'OWNER','2026-02-26 10:15:42'),(5,10,'MEMBER','2026-03-06 12:17:32'),(6,2,'MEMBER','2026-03-02 14:00:07'),(6,3,'OWNER','2026-02-26 13:58:38'),(7,1,'OWNER','2026-02-27 14:09:56'),(8,3,'OWNER','2026-03-02 08:00:31'),(9,3,'OWNER','2026-03-02 08:00:43'),(10,1,'OWNER','2026-03-06 07:37:19'),(11,2,'MEMBER','2026-03-31 11:50:10'),(11,3,'OWNER','2026-03-31 11:49:19');
/*!40000 ALTER TABLE `board_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `boards`
--

DROP TABLE IF EXISTS `boards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `boards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_boards_archived_at` (`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boards`
--

LOCK TABLES `boards` WRITE;
/*!40000 ALTER TABLE `boards` DISABLE KEYS */;
INSERT INTO `boards` VALUES (1,'Moj prvi board','2026-02-25 07:42:02',NULL),(5,'novi','2026-02-26 10:15:42',NULL),(6,'najnovije','2026-02-26 13:58:38',NULL),(7,'hej','2026-02-27 14:09:56',NULL),(8,'a','2026-03-02 08:00:31','2026-03-02 12:37:17'),(9,'v','2026-03-02 08:00:43','2026-03-02 09:10:03'),(10,'projekat','2026-03-06 07:37:19',NULL),(11,'moj','2026-03-31 11:49:19',NULL);
/*!40000 ALTER TABLE `boards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card_activity`
--

DROP TABLE IF EXISTS `card_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card_activity`
--

LOCK TABLES `card_activity` WRITE;
/*!40000 ALTER TABLE `card_activity` DISABLE KEYS */;
INSERT INTO `card_activity` VALUES (1,9,3,'marija@gmail.com','CREATED',NULL,'Task: logic','2026-03-02 12:24:09'),(2,9,3,'marija@gmail.com','ASSIGNED',NULL,'marija@gmail.com','2026-03-02 12:24:09'),(3,9,3,'marija@gmail.com','DUE_CHANGED','—','03.04.2026 05:00','2026-03-02 12:24:09'),(4,9,3,'marija@gmail.com','PRIORITY_CHANGED','P1','P5','2026-03-02 12:24:09'),(5,9,3,'marija@gmail.com','PRIORITY_CHANGED','P5','P1','2026-03-02 12:24:49'),(6,9,3,'marija@gmail.com','UNASSIGNED','marija@gmail.com',NULL,'2026-03-02 12:25:10'),(7,9,3,'marija@gmail.com','ASSIGNED',NULL,'marija@gmail.com','2026-03-02 12:25:14'),(8,9,3,'marija@gmail.com','MOVED_LIST','To do','Doing','2026-03-02 12:30:56'),(9,9,3,'marija@gmail.com','MOVED_LIST','Doing','To do','2026-03-02 12:31:03'),(10,9,3,'marija@gmail.com','MOVED_LIST','To do','Done','2026-03-02 12:37:05'),(11,10,3,'marija@gmail.com','CREATED',NULL,'Task: ai','2026-03-02 13:59:21'),(12,10,3,'marija@gmail.com','ASSIGNED',NULL,'marija@gmail.com','2026-03-02 13:59:21'),(13,10,3,'marija@gmail.com','DUE_CHANGED','—','02.04.2026 04:00','2026-03-02 13:59:21'),(14,10,3,'marija@gmail.com','COMMENTED',NULL,'komentar','2026-03-02 13:59:40'),(15,10,2,'milan@gmail.com','COMMENTED',NULL,'kom','2026-03-02 14:00:24'),(16,5,3,'marija@gmail.com','MOVED_LIST','To do','Doing','2026-03-04 11:11:43'),(17,5,3,'marija@gmail.com','MOVED_LIST','Doing','To do','2026-03-04 11:11:45'),(18,4,3,'marija@gmail.com','MOVED_LIST','To do','Doing','2026-03-04 11:12:14'),(19,4,3,'marija@gmail.com','MOVED_LIST','Doing','To do','2026-03-04 11:12:18'),(20,4,3,'marija@gmail.com','UNASSIGNED','marija@gmail.com',NULL,'2026-03-04 11:12:32'),(21,4,3,'marija@gmail.com','ASSIGNED',NULL,'marija@gmail.com','2026-03-04 11:12:33'),(22,2,3,'marija@gmail.com','UPDATED',NULL,'Promijenjeni detalji (naslov/opis).','2026-03-04 11:13:01'),(23,7,1,'admin@local','MOVED_LIST','To do','Done','2026-03-05 11:49:02'),(24,11,3,'marija@gmail.com','CREATED',NULL,'Task: aaaaaaa','2026-03-31 11:49:59'),(25,11,3,'marija@gmail.com','DUE_CHANGED','—','02.07.2026 03:00','2026-03-31 11:49:59'),(26,11,3,'marija@gmail.com','PRIORITY_CHANGED','P1','P5','2026-03-31 11:49:59'),(27,11,3,'marija@gmail.com','ASSIGNED',NULL,'milan@gmail.com','2026-03-31 11:50:18'),(28,11,3,'marija@gmail.com','COMMENTED',NULL,'a','2026-03-31 11:50:28'),(29,11,3,'marija@gmail.com','UPDATED',NULL,'Dodan attachment: Milan.docx','2026-03-31 11:56:16'),(30,11,3,'marija@gmail.com','UPDATED',NULL,'Obrisan attachment: Milan.docx','2026-03-31 11:56:24'),(31,11,3,'marija@gmail.com','UPDATED',NULL,'Dodan attachment: Milan.docx','2026-03-31 12:00:09'),(32,11,3,'marija@gmail.com','MOVED_LIST','To do','Doing','2026-03-31 12:10:00'),(33,11,3,'marija@gmail.com','MOVED_LIST','Doing','To do','2026-03-31 12:10:01'),(34,11,3,'marija@gmail.com','COMMENTED',NULL,'j','2026-03-31 12:10:12'),(35,11,3,'marija@gmail.com','UPDATED',NULL,'Dodan attachment: Milanov.docx','2026-03-31 12:10:20'),(36,11,3,'marija@gmail.com','UPDATED',NULL,'Obrisan attachment: Milanov.docx','2026-03-31 12:12:41'),(37,11,3,'marija@gmail.com','UPDATED',NULL,'Obrisan attachment: Milan.docx','2026-03-31 12:12:50'),(38,11,3,'marija@gmail.com','COMMENTED',NULL,'o','2026-03-31 12:12:55'),(39,11,3,'marija@gmail.com','UPDATED',NULL,'Dodan attachment: УНИВЕРЗИТЕТ У БАЊОЈ ЛУЦИ.docx','2026-03-31 12:13:11'),(40,12,3,'marija@gmail.com','CREATED',NULL,'Task: cv','2026-03-31 13:43:19'),(41,12,3,'marija@gmail.com','ASSIGNED',NULL,'milan@gmail.com','2026-03-31 13:43:19'),(42,12,3,'marija@gmail.com','DUE_CHANGED','—','03.04.2026 03:00','2026-03-31 13:43:19'),(43,12,3,'marija@gmail.com','PRIORITY_CHANGED','P1','P3','2026-03-31 13:43:19'),(44,13,2,'milan@gmail.com','CREATED',NULL,'Task: x','2026-03-31 13:44:03'),(45,13,2,'milan@gmail.com','ASSIGNED',NULL,'marija@gmail.com','2026-03-31 13:44:03'),(46,13,2,'milan@gmail.com','DUE_CHANGED','—','03.04.2026 02:00','2026-03-31 13:44:03');
/*!40000 ALTER TABLE `card_activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card_attachments`
--

DROP TABLE IF EXISTS `card_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card_attachments`
--

LOCK TABLES `card_attachments` WRITE;
/*!40000 ALTER TABLE `card_attachments` DISABLE KEYS */;
INSERT INTO `card_attachments` VALUES (5,10,3,'lit.txt','4a7db1e3-da50-4d27-b5a0-ee5cede41796.txt','text/plain',2255,'2026-03-19 12:27:32'),(12,10,3,'Milan.docx','8e384268-611b-44bd-9e17-e2bcab1c8bb5.docx','application/vnd.openxmlformats-officedocument.wordprocessingml.document',20220,'2026-03-31 11:32:29'),(13,10,3,'Milanov.docx','14a28b3e-3680-41d2-94a1-c40b0070acc0.docx','application/vnd.openxmlformats-officedocument.wordprocessingml.document',64527,'2026-03-31 11:32:37'),(14,11,3,'lit.txt','ffd4ccdd-0159-4242-ae75-70e971100101.txt','text/plain',2255,'2026-03-31 11:50:44'),(18,11,3,'УНИВЕРЗИТЕТ У БАЊОЈ ЛУЦИ.docx','3998aca5-9ce1-449d-95f2-e8b11474c968.docx','application/vnd.openxmlformats-officedocument.wordprocessingml.document',21618,'2026-03-31 12:13:11');
/*!40000 ALTER TABLE `card_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `card_comments`
--

DROP TABLE IF EXISTS `card_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `card_comments`
--

LOCK TABLES `card_comments` WRITE;
/*!40000 ALTER TABLE `card_comments` DISABLE KEYS */;
INSERT INTO `card_comments` VALUES (1,10,3,'komentar','2026-03-02 12:59:41'),(2,10,2,'kom','2026-03-02 13:00:24'),(3,11,3,'a','2026-03-31 09:50:28'),(4,11,3,'j','2026-03-31 10:10:13'),(5,11,3,'o','2026-03-31 10:12:55');
/*!40000 ALTER TABLE `card_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cards`
--

DROP TABLE IF EXISTS `cards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  PRIMARY KEY (`id`),
  KEY `fk_card_created_by` (`created_by`),
  KEY `fk_card_assigned_to` (`assigned_to`),
  KEY `idx_cards_list_pos` (`list_id`,`position`),
  KEY `idx_cards_board` (`board_id`),
  CONSTRAINT `fk_card_assigned_to` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_card_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`),
  CONSTRAINT `fk_card_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_card_list` FOREIGN KEY (`list_id`) REFERENCES `lists` (`id`),
  CONSTRAINT `chk_cards_priority` CHECK ((`priority` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cards`
--

LOCK TABLES `cards` WRITE;
/*!40000 ALTER TABLE `cards` DISABLE KEYS */;
INSERT INTO `cards` VALUES (1,1,1,'Prvi task','Ovo je test kartica','2026-03-04 19:00:00',1,NULL,1000.000000,NULL,'2026-02-25 07:42:02','2026-02-26 13:51:33',1),(2,5,5,'zad','uradis','2026-02-27 05:00:00',3,3,1000.000000,NULL,'2026-02-26 10:29:43','2026-03-04 11:13:01',1),(3,5,6,'zad2','uradi2','2026-02-27 20:00:00',3,3,2000.000000,NULL,'2026-02-26 10:35:50','2026-02-26 14:39:42',1),(4,5,4,'z','z',NULL,3,3,2000.000000,NULL,'2026-02-26 10:51:58','2026-03-04 11:12:33',1),(5,5,4,'x','x',NULL,3,2,3000.000000,NULL,'2026-02-26 11:10:25','2026-03-04 11:11:45',1),(6,5,6,'gotov','log',NULL,2,2,1000.000000,NULL,'2026-02-26 11:13:17','2026-02-26 11:13:17',1),(7,7,12,'a','a','2026-03-06 02:00:00',1,1,1000.000000,NULL,'2026-02-27 14:10:08','2026-03-05 11:49:03',5),(8,9,18,'moj','a',NULL,3,3,1000.000000,NULL,'2026-03-02 09:09:57','2026-03-02 09:09:57',1),(9,8,15,'logic','log','2026-04-03 03:00:00',3,3,1000.000000,NULL,'2026-03-02 12:24:09','2026-03-02 12:37:05',1),(10,6,7,'ai','ai','2026-04-02 02:00:00',3,3,1000.000000,NULL,'2026-03-02 13:59:21','2026-03-02 13:59:21',1),(11,11,22,'aaaaaaa','aaaaaa','2026-07-02 01:00:00',3,2,1000.000000,NULL,'2026-03-31 11:49:59','2026-03-31 12:10:01',5),(12,11,22,'cv','cv','2026-04-03 01:00:00',3,2,2000.000000,NULL,'2026-03-31 13:43:19','2026-03-31 13:43:19',3),(13,11,22,'x','x','2026-04-03 00:00:00',2,3,3000.000000,NULL,'2026-03-31 13:44:03','2026-03-31 13:44:03',1);
/*!40000 ALTER TABLE `cards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lists`
--

DROP TABLE IF EXISTS `lists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` decimal(18,6) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lists_board_pos` (`board_id`,`position`),
  CONSTRAINT `fk_list_board` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lists`
--

LOCK TABLES `lists` WRITE;
/*!40000 ALTER TABLE `lists` DISABLE KEYS */;
INSERT INTO `lists` VALUES (1,1,'To do',1000.000000,'2026-02-25 07:42:02'),(2,1,'Doing',2000.000000,'2026-02-25 07:42:02'),(3,1,'Done',3000.000000,'2026-02-25 07:42:02'),(4,5,'To do',1000.000000,'2026-02-26 10:29:20'),(5,5,'Doing',2000.000000,'2026-02-26 10:29:20'),(6,5,'Done',3000.000000,'2026-02-26 10:29:20'),(7,6,'To do',1000.000000,'2026-02-26 13:58:38'),(8,6,'Doing',2000.000000,'2026-02-26 13:58:38'),(9,6,'Done',3000.000000,'2026-02-26 13:58:38'),(10,7,'To do',1000.000000,'2026-02-27 14:09:56'),(11,7,'Doing',2000.000000,'2026-02-27 14:09:56'),(12,7,'Done',3000.000000,'2026-02-27 14:09:56'),(13,8,'To do',1000.000000,'2026-03-02 08:00:31'),(14,8,'Doing',2000.000000,'2026-03-02 08:00:31'),(15,8,'Done',3000.000000,'2026-03-02 08:00:31'),(16,9,'To do',1000.000000,'2026-03-02 08:00:43'),(17,9,'Doing',2000.000000,'2026-03-02 08:00:43'),(18,9,'Done',3000.000000,'2026-03-02 08:00:43'),(19,10,'To do',1000.000000,'2026-03-06 07:37:19'),(20,10,'Doing',2000.000000,'2026-03-06 07:37:19'),(21,10,'Done',3000.000000,'2026-03-06 07:37:19'),(22,11,'To do',1000.000000,'2026-03-31 11:49:19'),(23,11,'Doing',2000.000000,'2026-03-31 11:49:19'),(24,11,'Done',3000.000000,'2026-03-31 11:49:19');
/*!40000 ALTER TABLE `lists` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,2,'TASK_ASSIGNED','Dodijeljen vam je novi task','Marija Aleksic vam je dodijelio task: cv',11,12,3,_binary '','2026-03-31 13:43:19','2026-03-31 11:43:43'),(2,3,'TASK_ASSIGNED','Dodijeljen vam je novi task','Milan Aleksic vam je dodijelio task: x',11,13,2,_binary '','2026-03-31 13:44:03','2026-03-31 11:48:11');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `must_change_password` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin@local','$2a$10$SQMN8o9zPsknyerAUtz0G.U2BaqwmRU6nLFPHBHnmwaF5hXYGGyKe','Admin','2026-02-25 07:42:02',0),(2,'milan@gmail.com','$2a$10$XVZam9pH8HrB4LF6Udl6sOce1NLjuOpaTFm5Y/EY8OUmEdVddRKL2','Milan Aleksic','2026-02-25 14:31:20',0),(3,'marija@gmail.com','$2a$10$PN//RKX6CIch03QvF.Gl4OLEekj1fLkdCfEs2msWxhGSmKp1Lw4AG','Marija Aleksic','2026-02-25 14:34:41',0),(6,'marko@gmail.com','$2a$10$F0xKlNU3YuiJk6XPU7qjme4V1jz7jV6QZhEHowCQBjmMlpTr/dNpS','Marko Markovic','2026-03-03 11:48:40',1),(8,'dragan@gmail.com','$2a$10$AZJZJmw4dOIBKezxSlpSyOTMgardwJi3Ob.KwGsisD2C5sMctCrr6','Dragan Culum','2026-03-06 07:37:41',1),(9,'milos@gmail.com','$2a$10$rPAgeZ/6JSpim442Az.KPupnV/OI8PoypXRsifdWmEPfwiw/KrHQW','Milos Milosevic','2026-03-06 11:47:40',1),(10,'petar@gmail.com','$2a$10$0dCVfKcyg3vLLhiPfvUEQu16rQF3H/1amTRmDF2zqLiepleU7/giq','Petar Petrovic','2026-03-06 11:49:12',0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'task_app'
--

--
-- Dumping routines for database 'task_app'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-31 15:49:38
