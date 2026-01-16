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
-- Table structure for table `visit`
--

DROP TABLE IF EXISTS `visit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `visit` (
  `visit_id` int NOT NULL AUTO_INCREMENT,
  `activity_id` int NOT NULL,
  `table_id` varchar(10) NOT NULL,
  `actual_start_time` datetime NOT NULL,
  `actual_end_time` datetime NOT NULL,
  PRIMARY KEY (`visit_id`),
  KEY `activity_id` (`activity_id`),
  KEY `table_id` (`table_id`),
  CONSTRAINT `visit_ibfk_1` FOREIGN KEY (`activity_id`) REFERENCES `user_activity` (`activity_id`),
  CONSTRAINT `visit_ibfk_2` FOREIGN KEY (`table_id`) REFERENCES `restaurant_table` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visit`
--

LOCK TABLES `visit` WRITE;
/*!40000 ALTER TABLE `visit` DISABLE KEYS */;
INSERT INTO `visit` VALUES (1,1,'T01','2025-12-28 11:05:09','2025-12-28 12:05:09'),(2,2,'T02','2025-12-28 11:05:09','2025-12-28 12:05:09'),(3,3,'T03','2025-12-28 11:05:09','2025-12-28 12:05:09'),(4,4,'T04','2025-12-28 11:05:09','2025-12-28 12:05:09'),(5,5,'T05','2025-12-28 11:05:09','2025-12-28 12:05:09'),(6,6,'T06','2025-12-28 11:05:09','2025-12-28 12:05:09'),(7,7,'T07','2025-12-28 11:05:09','2025-12-28 12:05:09'),(8,8,'T08','2025-12-28 11:05:09','2025-12-28 12:05:09'),(9,9,'T09','2025-12-28 11:05:09','2025-12-28 12:05:09'),(10,10,'T10','2025-12-28 11:05:09','2025-12-28 12:05:09');
/*!40000 ALTER TABLE `visit` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-28 13:45:44
