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
-- Table structure for table `performance_log`
--

DROP TABLE IF EXISTS `performance_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `performance_log` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `visit_id` int NOT NULL,
  `late_minutes` int DEFAULT '0',
  `overstay_minutes` int DEFAULT '0',
  `report_date` date NOT NULL,
  PRIMARY KEY (`log_id`),
  KEY `visit_id` (`visit_id`),
  CONSTRAINT `performance_log_ibfk_1` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`visit_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `performance_log`
--

LOCK TABLES `performance_log` WRITE;
/*!40000 ALTER TABLE `performance_log` DISABLE KEYS */;
INSERT INTO `performance_log` VALUES (1,1,0,0,'2025-12-20'),(2,2,5,0,'2025-12-21'),(3,3,0,10,'2025-12-22'),(4,4,3,0,'2025-12-06'),(5,5,0,0,'2025-12-23'),(6,6,2,8,'2025-12-24'),(7,7,0,0,'2025-12-25'),(8,8,4,0,'2025-12-07'),(9,9,0,6,'2025-12-26'),(10,10,1,0,'2025-12-27');
/*!40000 ALTER TABLE `performance_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-16 19:46:41
