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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_activity`
--

LOCK TABLES `user_activity` WRITE;
/*!40000 ALTER TABLE `user_activity` DISABLE KEYS */;
INSERT INTO `user_activity` VALUES (1,'user1',NULL,NULL,1,NULL,'2025-12-28 11:05:09'),(2,'user1',NULL,NULL,2,NULL,'2025-12-28 11:05:09'),(3,'user2',NULL,NULL,3,NULL,'2025-12-28 11:05:09'),(4,NULL,'0509999999','guest@mail.com',4,NULL,'2025-12-28 11:05:09'),(5,'user3',NULL,NULL,5,NULL,'2025-12-28 11:05:09'),(6,'user4',NULL,NULL,6,NULL,'2025-12-28 11:05:09'),(7,NULL,'0508888888','guest2@mail.com',7,NULL,'2025-12-28 11:05:09'),(8,'user5',NULL,NULL,8,NULL,'2025-12-28 11:05:09'),(9,NULL,'0507777777','guest3@mail.com',9,NULL,'2025-12-28 11:05:09'),(10,'user1',NULL,NULL,10,NULL,'2025-12-28 11:05:09');
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

-- Dump completed on 2025-12-28 13:45:46
