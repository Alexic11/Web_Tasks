CREATE DATABASE  IF NOT EXISTS `task_app` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `task_app`;
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
INSERT INTO `board_members` VALUES (1,1,'OWNER','2026-02-25 07:42:02'),(1,3,'MEMBER','2026-02-26 08:11:06'),(5,2,'MEMBER','2026-02-26 11:10:15'),(5,3,'OWNER','2026-02-26 10:15:42'),(6,3,'OWNER','2026-02-26 13:58:38'),(7,1,'OWNER','2026-02-27 14:09:56');
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boards`
--

LOCK TABLES `boards` WRITE;
/*!40000 ALTER TABLE `boards` DISABLE KEYS */;
INSERT INTO `boards` VALUES (1,'Moj prvi board','2026-02-25 07:42:02'),(5,'novi','2026-02-26 10:15:42'),(6,'najnovije','2026-02-26 13:58:38'),(7,'hej','2026-02-27 14:09:56');
/*!40000 ALTER TABLE `boards` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cards`
--

LOCK TABLES `cards` WRITE;
/*!40000 ALTER TABLE `cards` DISABLE KEYS */;
INSERT INTO `cards` VALUES (1,1,1,'Prvi task','Ovo je test kartica','2026-03-04 19:00:00',1,NULL,1000.000000,NULL,'2026-02-25 07:42:02','2026-02-26 13:51:33',1),(2,5,6,'zad','uradi','2026-02-27 05:00:00',3,3,1000.000000,NULL,'2026-02-26 10:29:43','2026-02-26 13:38:54',1),(3,5,6,'zad2','uradi2','2026-02-27 20:00:00',3,3,2000.000000,NULL,'2026-02-26 10:35:50','2026-02-26 14:39:42',1),(4,5,4,'z','z',NULL,3,3,2000.000000,NULL,'2026-02-26 10:51:58','2026-02-26 11:09:26',1),(5,5,4,'x','x',NULL,3,2,3000.000000,NULL,'2026-02-26 11:10:25','2026-02-26 11:11:07',1),(6,5,6,'gotov','log',NULL,2,2,1000.000000,NULL,'2026-02-26 11:13:17','2026-02-26 11:13:17',1),(7,7,10,'a','a','2026-03-06 02:00:00',1,1,1000.000000,NULL,'2026-02-27 14:10:08','2026-02-27 15:00:54',5);
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
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lists`
--

LOCK TABLES `lists` WRITE;
/*!40000 ALTER TABLE `lists` DISABLE KEYS */;
INSERT INTO `lists` VALUES (1,1,'To do',1000.000000,'2026-02-25 07:42:02'),(2,1,'Doing',2000.000000,'2026-02-25 07:42:02'),(3,1,'Done',3000.000000,'2026-02-25 07:42:02'),(4,5,'To do',1000.000000,'2026-02-26 10:29:20'),(5,5,'Doing',2000.000000,'2026-02-26 10:29:20'),(6,5,'Done',3000.000000,'2026-02-26 10:29:20'),(7,6,'To do',1000.000000,'2026-02-26 13:58:38'),(8,6,'Doing',2000.000000,'2026-02-26 13:58:38'),(9,6,'Done',3000.000000,'2026-02-26 13:58:38'),(10,7,'To do',1000.000000,'2026-02-27 14:09:56'),(11,7,'Doing',2000.000000,'2026-02-27 14:09:56'),(12,7,'Done',3000.000000,'2026-02-27 14:09:56');
/*!40000 ALTER TABLE `lists` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin@local','$2a$10$SQMN8o9zPsknyerAUtz0G.U2BaqwmRU6nLFPHBHnmwaF5hXYGGyKe','Admin','2026-02-25 07:42:02',0),(2,'milan@gmail.com','$2a$10$XVZam9pH8HrB4LF6Udl6sOce1NLjuOpaTFm5Y/EY8OUmEdVddRKL2','Milan Aleksic','2026-02-25 14:31:20',0),(3,'marija@gmail.com','$2a$10$PN//RKX6CIch03QvF.Gl4OLEekj1fLkdCfEs2msWxhGSmKp1Lw4AG','Marija Aleksic','2026-02-25 14:34:41',0),(4,'culum@m','$2a$10$DYSw5cn6aM/LBn/TxrReoe7aA3GQTcTdz.XB0HgAoiZZG3fdK6dCO','d c','2026-02-26 07:08:39',0);
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

-- Dump completed on 2026-03-02  8:09:57
