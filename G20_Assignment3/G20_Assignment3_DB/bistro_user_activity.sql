CREATE DATABASE  IF NOT EXISTS `bistro` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `bistro`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: bistro
-- ------------------------------------------------------
-- Server version	8.0.44

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
-- Table structure for table `user_activity`
--

DROP TABLE IF EXISTS `user_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_activity` (
  `activity_id` int NOT NULL AUTO_INCREMENT,
  `subscriber_username` varchar(50) DEFAULT NULL,
  `guest_phone` varchar(20) DEFAULT NULL,
  `guest_email` varchar(100) DEFAULT NULL,
  `reservation_id` int DEFAULT NULL,
  `waiting_id` int DEFAULT NULL,
  `activity_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`activity_id`),
  KEY `subscriber_username` (`subscriber_username`),
  KEY `reservation_id` (`reservation_id`),
  KEY `waiting_id` (`waiting_id`),
  CONSTRAINT `user_activity_ibfk_1` FOREIGN KEY (`subscriber_username`) REFERENCES `subscribers` (`username`) ON DELETE SET NULL,
  CONSTRAINT `user_activity_ibfk_2` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`reservation_id`),
  CONSTRAINT `user_activity_ibfk_3` FOREIGN KEY (`waiting_id`) REFERENCES `waiting_list` (`waiting_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_activity`
--

LOCK TABLES `user_activity` WRITE;
/*!40000 ALTER TABLE `user_activity` DISABLE KEYS */;
INSERT INTO `user_activity` VALUES (1,'user1',NULL,NULL,1,NULL,'2025-12-20 11:01:09'),(2,'user1',NULL,NULL,2,NULL,'2025-12-21 11:02:09'),(3,'user2',NULL,NULL,3,NULL,'2025-12-22 11:03:09'),(4,NULL,'0509999999','guest@mail.com',NULL,6,'2025-12-06 11:06:09'),(5,'user3',NULL,NULL,4,NULL,'2025-12-23 11:04:09'),(6,'user4',NULL,NULL,5,NULL,'2025-12-24 11:05:09'),(7,'user4',NULL,NULL,6,NULL,'2025-12-25 11:06:09'),(8,NULL,'0508888888','guest2@mail.com',NULL,7,'2025-12-07 11:07:09'),(9,'user5',NULL,NULL,7,NULL,'2025-12-26 11:07:09'),(10,'user5',NULL,NULL,8,NULL,'2025-12-27 11:08:09'),(11,NULL,'0507777777','guest3@mail.com',NULL,9,'2025-12-09 11:09:09'),(12,'user6',NULL,NULL,9,NULL,'2025-12-28 11:09:09'),(13,'user6',NULL,NULL,10,NULL,'2025-12-29 11:10:09'),(14,'user11',NULL,NULL,11,NULL,'2025-12-01 11:05:12'),(15,'user12',NULL,NULL,12,NULL,'2025-12-02 11:05:13'),(16,'user13',NULL,NULL,13,NULL,'2025-12-03 11:05:14'),(17,'user14',NULL,NULL,14,NULL,'2025-12-04 11:05:15'),(18,'user15',NULL,NULL,15,NULL,'2025-12-05 11:05:16'),(19,'user16',NULL,NULL,16,NULL,'2025-12-06 11:05:17'),(20,'user17',NULL,NULL,17,NULL,'2025-12-07 11:05:18'),(21,'user18',NULL,NULL,18,NULL,'2025-12-08 11:05:19'),(22,'user19',NULL,NULL,19,NULL,'2025-12-09 11:05:20'),(23,'user20',NULL,NULL,20,NULL,'2025-12-20 11:05:21'),(24,NULL,'0507777776','guest3@mail.com',NULL,10,'2025-12-10 11:10:09'),(25,NULL,'0507777775','guest3@mail.com',NULL,11,'2025-12-11 11:11:09'),(26,NULL,'0507777774','guest3@mail.com',NULL,12,'2025-12-12 11:12:09'),(27,NULL,'0507777773','guest3@mail.com',NULL,13,'2025-12-13 11:13:09'),(28,NULL,'0507777772','guest3@mail.com',NULL,14,'2025-12-14 11:14:09');
/*!40000 ALTER TABLE `user_activity` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-17 18:40:18
