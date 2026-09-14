-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: university
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `advisor`
--

DROP TABLE IF EXISTS `advisor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `advisor` (
  `s_id` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `i_id` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`s_id`),
  KEY `fk_advisor_instructor` (`i_id`),
  CONSTRAINT `fk_advisor_instructor` FOREIGN KEY (`i_id`) REFERENCES `instructor` (`ID`),
  CONSTRAINT `fk_advisor_student` FOREIGN KEY (`s_id`) REFERENCES `student` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='advisor';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `advisor`
--

LOCK TABLES `advisor` WRITE;
/*!40000 ALTER TABLE `advisor` DISABLE KEYS */;
INSERT INTO `advisor` VALUES ('12345','10101'),('44553','22222'),('45678','22222'),('00128','45565'),('54321','45565'),('76543','45565'),('23121','76543'),('98988','76766'),('76653','98345'),('98765','98345');
/*!40000 ALTER TABLE `advisor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计记录ID（自增主键）',
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作者登录账号；无请求上下文（如定时任务/测试）为 NULL',
  `action` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/GRADE_UPDATE/ACCOUNT_CREATE/PASSWORD_RESET/ACCOUNT_TOGGLE/ACCOUNT_DELETE/ACCOUNT_BATCH_CREATE',
  `target_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象类型：DEPARTMENT/COURSE/INSTRUCTOR/STUDENT/CLASSROOM/SECTION/PREREQ/ACCOUNT/GRADE',
  `target_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对象标识（业务主键或组合键）',
  `detail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作详情（含变更前后值，供追溯）',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作来源 IP（TCP 对端地址，不读可伪造的 X-Forwarded-For）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_created` (`created_at`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_target` (`target_type`,`target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=675 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审计日志（敏感操作留痕，只增不改）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
INSERT INTO `audit_log` VALUES (29,'admin','PASSWORD_RESET','ACCOUNT','98345','重置密码：98345','0:0:0:0:0:0:0:1','2026-08-12 00:42:28'),(30,'admin','PASSWORD_RESET','ACCOUNT','10101','重置密码：10101','0:0:0:0:0:0:0:1','2026-08-12 00:42:36'),(31,'admin','ACCOUNT_TOGGLE','ACCOUNT','98345','禁用账号：98345','0:0:0:0:0:0:0:1','2026-08-12 00:42:53'),(67,'admin','UPDATE','SECTION','MU-199/2/Spring/2010','取消排课：课程 MU-199，班 2，Spring 2010（教室/时间段清空）','0:0:0:0:0:0:0:1','2026-08-14 14:37:19'),(68,'admin','UPDATE','SECTION','MU-199/2/Spring/2010','排课：课程 MU-199，班 2，Spring 2010，教室 Watson 120，时段 B，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:38:03'),(69,'admin','UPDATE','SECTION','HIS-351/1/Spring/2010','排课：课程 HIS-351，班 1，Spring 2010，教室 Painter 514，时段 D，授课教师 32343','0:0:0:0:0:0:0:1','2026-08-14 14:38:16'),(70,'admin','UPDATE','SECTION','MU-199/1/Spring/2010','排课：课程 MU-199，班 1，Spring 2010，教室 Painter 102，时段 D，授课教师 15151','0:0:0:0:0:0:0:1','2026-08-14 14:38:18'),(71,'admin','UPDATE','SECTION','HIS-351/1/Spring/2010','排课：课程 HIS-351，班 1，Spring 2010，教室 Painter 514，时段 F，授课教师 32343','0:0:0:0:0:0:0:1','2026-08-14 14:38:19'),(72,'admin','UPDATE','SECTION','MU-199/1/Spring/2010','排课：课程 MU-199，班 1，Spring 2010，教室 Painter 102，时段 F，授课教师 15151','0:0:0:0:0:0:0:1','2026-08-14 14:38:21'),(73,'admin','UPDATE','SECTION','HIS-351/1/Spring/2010','排课：课程 HIS-351，班 1，Spring 2010，教室 Taylor 3128，时段 F，授课教师 32343','0:0:0:0:0:0:0:1','2026-08-14 14:38:23'),(74,'admin','UPDATE','SECTION','HIS-351/1/Spring/2010','排课：课程 HIS-351，班 1，Spring 2010，教室 Watson 100，时段 F，授课教师 32343','0:0:0:0:0:0:0:1','2026-08-14 14:38:25'),(75,'admin','UPDATE','SECTION','CS-101/1/Spring/2010','排课：课程 CS-101，班 1，Spring 2010，教室 Taylor 3128，时段 G，授课教师 45565','0:0:0:0:0:0:0:1','2026-08-14 14:38:27'),(76,'admin','UPDATE','SECTION','CS-315/1/Spring/2010','排课：课程 CS-315，班 1，Spring 2010，教室 Painter 514，时段 G，授课教师 10101','0:0:0:0:0:0:0:1','2026-08-14 14:38:28'),(77,'admin','UPDATE','SECTION','CS-101/1/Spring/2010','排课：课程 CS-101，班 1，Spring 2010，教室 Watson 100，时段 G，授课教师 45565','0:0:0:0:0:0:0:1','2026-08-14 14:38:34'),(78,'admin','UPDATE','SECTION','CS-101/1/Spring/2010','排课：课程 CS-101，班 1，Spring 2010，教室 Watson 120，时段 G，授课教师 45565','0:0:0:0:0:0:0:1','2026-08-14 14:38:35'),(79,'admin','UPDATE','SECTION','CS-319/1/Spring/2010','排课：课程 CS-319，班 1，Spring 2010，教室 Taylor 3128，时段 C，授课教师 45565','0:0:0:0:0:0:0:1','2026-08-14 14:38:50'),(80,'admin','UPDATE','SECTION','CS-319/1/Spring/2010','排课：课程 CS-319，班 1，Spring 2010，教室 Painter 514，时段 C，授课教师 45565','0:0:0:0:0:0:0:1','2026-08-14 14:38:52'),(81,'admin','UPDATE','SECTION','BIO-101/3/Spring/2010','排课：课程 BIO-101，班 3，Spring 2010，教室 Watson 100，时段 B，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:39:23'),(82,'admin','UPDATE','SECTION','BIO-101/3/Spring/2010','排课：课程 BIO-101，班 3，Spring 2010，教室 Taylor 3128，时段 B，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:39:24'),(83,'admin','UPDATE','SECTION','BIO-101/3/Spring/2010','排课：课程 BIO-101，班 3，Spring 2010，教室 Painter 514，时段 B，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:39:26'),(84,'admin','UPDATE','SECTION','CS-319/1/Spring/2010','排课：课程 CS-319，班 1，Spring 2010，教室 Painter 514，时段 D，授课教师 45565','0:0:0:0:0:0:0:1','2026-08-14 14:39:43'),(85,'admin','UPDATE','SECTION','CS-319/1/Spring/2010','排课：课程 CS-319，班 1，Spring 2010，教室 Painter 102，时段 D，授课教师 45565','0:0:0:0:0:0:0:1','2026-08-14 14:39:44'),(86,'admin','UPDATE','SECTION','ENG-101/2/Spring/2010','排课：课程 ENG-101，班 2，Spring 2010，教室 Packard 101，时段 C，授课教师 98345','0:0:0:0:0:0:0:1','2026-08-14 14:39:45'),(87,'admin','UPDATE','SECTION','MU-199/1/Spring/2010','排课：课程 MU-199，班 1，Spring 2010，教室 Painter 102，时段 G，授课教师 15151','0:0:0:0:0:0:0:1','2026-08-14 14:40:00'),(88,'admin','UPDATE','SECTION','MU-199/1/Spring/2010','排课：课程 MU-199，班 1，Spring 2010，教室 Painter 102，时段 H，授课教师 15151','0:0:0:0:0:0:0:1','2026-08-14 14:40:01'),(89,'admin','UPDATE','SECTION','MU-199/1/Spring/2010','排课：课程 MU-199，班 1，Spring 2010，教室 Packard 101，时段 H，授课教师 15151','0:0:0:0:0:0:0:1','2026-08-14 14:40:06'),(90,'admin','UPDATE','SECTION','FIN-201/1/Spring/2010','排课：课程 FIN-201，班 1，Spring 2010，教室 Packard 101，时段 B，授课教师 12121','0:0:0:0:0:0:0:1','2026-08-14 14:40:09'),(91,'admin','UPDATE','SECTION','EN-101/1/Spring/2010','排课：课程 EN-101，班 1，Spring 2010，教室 Painter 102，时段 B，授课教师 98345','0:0:0:0:0:0:0:1','2026-08-14 14:40:11'),(92,'admin','UPDATE','SECTION','BIO-101/3/Spring/2010','排课：课程 BIO-101，班 3，Spring 2010，教室 Painter 514，时段 A，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:40:14'),(93,'admin','UPDATE','SECTION','EN-101/1/Spring/2010','排课：课程 EN-101，班 1，Spring 2010，教室 Painter 102，时段 A，授课教师 98345','0:0:0:0:0:0:0:1','2026-08-14 14:40:16'),(94,'admin','UPDATE','SECTION','FIN-201/1/Spring/2010','排课：课程 FIN-201，班 1，Spring 2010，教室 Packard 101，时段 A，授课教师 12121','0:0:0:0:0:0:0:1','2026-08-14 14:40:17'),(95,'admin','UPDATE','SECTION','MU-199/2/Spring/2010','排课：课程 MU-199，班 2，Spring 2010，教室 Watson 120，时段 A，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:40:20'),(96,'admin','UPDATE','SECTION','MU-199/2/Spring/2010','排课：课程 MU-199，班 2，Spring 2010，教室 Watson 100，时段 A，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:40:21'),(97,'admin','UPDATE','SECTION','MU-199/2/Spring/2010','排课：课程 MU-199，班 2，Spring 2010，教室 Taylor 3128，时段 A，授课教师 无','0:0:0:0:0:0:0:1','2026-08-14 14:40:22'),(98,'admin','UPDATE','SECTION','CS-319/2/Spring/2010','排课：课程 CS-319，班 2，Spring 2010，教室 Taylor 3128，时段 C，授课教师 83821','0:0:0:0:0:0:0:1','2026-08-14 14:40:25'),(99,'admin','UPDATE','SECTION','CS-319/2/Spring/2010','排课：课程 CS-319，班 2，Spring 2010，教室 Painter 514，时段 C，授课教师 83821','0:0:0:0:0:0:0:1','2026-08-14 14:40:29'),(100,'admin','UPDATE','SECTION','CS-319/2/Spring/2010','排课：课程 CS-319，班 2，Spring 2010，教室 Painter 102，时段 C，授课教师 83821','0:0:0:0:0:0:0:1','2026-08-14 14:40:30'),(101,'admin','UPDATE','SECTION','CS-319/2/Spring/2010','排课：课程 CS-319，班 2，Spring 2010，教室 Painter 514，时段 C，授课教师 83821','0:0:0:0:0:0:0:1','2026-08-14 14:40:32'),(102,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Taylor 3128，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:40:35'),(103,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Watson 100，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:40:37'),(104,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Watson 120，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:40:38'),(105,'admin','UPDATE','SECTION','HIS-351/1/Spring/2010','排课：课程 HIS-351，班 1，Spring 2010，教室 Watson 100，时段 D，授课教师 32343','0:0:0:0:0:0:0:1','2026-08-14 14:40:39'),(106,'admin','UPDATE','SECTION','HIS-351/1/Spring/2010','排课：课程 HIS-351，班 1，Spring 2010，教室 Taylor 3128，时段 D，授课教师 32343','0:0:0:0:0:0:0:1','2026-08-14 14:40:41'),(107,'admin','UPDATE','SECTION','HIS-351/1/Spring/2010','排课：课程 HIS-351，班 1，Spring 2010，教室 Taylor 3128，时段 C，授课教师 32343','0:0:0:0:0:0:0:1','2026-08-14 14:40:41'),(108,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Watson 120，时段 B，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:40:48'),(109,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Watson 100，时段 B，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:41:05'),(110,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Watson 120，时段 B，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:41:08'),(111,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Watson 100，时段 B，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:41:10'),(112,'admin','UPDATE','SECTION','EN-101/2/Spring/2010','排课：课程 EN-101，班 2，Spring 2010，教室 Watson 120，时段 B，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:41:15'),(113,'admin','CREATE','SECTION','BIO-101/3/Spring/2011','新建待排课开课班：课程 BIO-101，班 3，Spring 2011，教室 无 无，时段 无，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:12'),(114,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 102，时段 A，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:21'),(115,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 102，时段 B，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:22'),(116,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 102，时段 C，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:23'),(117,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 514，时段 D，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:24'),(118,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 514，时段 C，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:25'),(119,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Taylor 3128，时段 C，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:26'),(120,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 100，时段 C，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:27'),(121,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 100，时段 D，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:28'),(122,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 100，时段 F，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:28'),(123,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Taylor 3128，时段 F，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:32'),(124,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 514，时段 F，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:33'),(125,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 102，时段 F，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:37'),(126,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Packard 101，时段 F，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:39'),(127,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Packard 101，时段 D，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:41'),(128,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Packard 101，时段 C，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:42'),(129,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Packard 101，时段 B，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:43'),(130,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Packard 101，时段 A，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:44'),(131,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 102，时段 A，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:47'),(132,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Painter 514，时段 A，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:47'),(133,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Taylor 3128，时段 A，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:48'),(134,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 100，时段 A，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:49'),(135,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 120，时段 A，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:50'),(136,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 120，时段 B，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:52'),(137,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 120，时段 C，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:52'),(138,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 120，时段 D，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:53'),(139,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 120，时段 F，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:54'),(140,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 120，时段 G，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:55'),(141,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Watson 100，时段 G，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:56'),(142,'admin','UPDATE','SECTION','BIO-101/3/Spring/2011','排课：课程 BIO-101，班 3，Spring 2011，教室 Taylor 3128，时段 G，授课教师 45678','0:0:0:0:0:0:0:1','2026-08-14 14:45:57'),(143,'admin','CREATE','SECTION','BIO-301/1/Spring/2011','新建待排课开课班：课程 BIO-301，班 1，Spring 2011，教室 无 无，时段 无，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:21'),(144,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:25'),(145,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 102，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:27'),(146,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 514，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:29'),(147,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Taylor 3128，时段 F，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:30'),(148,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Taylor 3128，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:34'),(149,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Taylor 3128，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:35'),(150,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 514，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:36'),(151,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 102，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:37'),(152,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:38'),(153,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 B，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:40'),(154,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:41'),(155,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 102，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:43'),(156,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 514，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:44'),(157,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Taylor 3128，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:45'),(158,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 100，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:46'),(159,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 120，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:47'),(160,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 120，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:54'),(161,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 100，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:55'),(162,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Taylor 3128，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:58'),(163,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 514，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:46:59'),(164,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 102，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:00'),(165,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:00'),(166,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:01'),(167,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 B，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:02'),(168,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Packard 101，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:03'),(169,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 102，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:04'),(170,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 514，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:05'),(171,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Taylor 3128，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:06'),(172,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 100，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:07'),(173,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 120，时段 A，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:08'),(174,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 120，时段 C，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:08'),(175,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 120，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:09'),(176,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Watson 100，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:12'),(177,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Taylor 3128，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:12'),(178,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 514，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:13'),(179,'admin','UPDATE','SECTION','BIO-301/1/Spring/2011','排课：课程 BIO-301，班 1，Spring 2011，教室 Painter 102，时段 D，授课教师 98765','0:0:0:0:0:0:0:1','2026-08-14 14:47:14'),(180,'zhang','AVATAR_UPDATE','ACCOUNT','zhang','更新头像：zhang','0:0:0:0:0:0:0:1','2026-08-14 15:21:42'),(181,'zhang','AVATAR_REMOVE','ACCOUNT','zhang','移除头像：zhang','0:0:0:0:0:0:0:1','2026-08-14 15:21:42'),(182,'zhang','AVATAR_UPDATE','ACCOUNT','zhang','更新头像：zhang','0:0:0:0:0:0:0:1','2026-08-14 15:27:42'),(183,'zhang','AVATAR_REMOVE','ACCOUNT','zhang','移除头像：zhang','0:0:0:0:0:0:0:1','2026-08-14 15:27:42'),(184,'zhang','AVATAR_UPDATE','ACCOUNT','zhang','更新头像：zhang','0:0:0:0:0:0:0:1','2026-08-14 15:43:13'),(185,'zhang','AVATAR_REMOVE','ACCOUNT','zhang','移除头像：zhang','0:0:0:0:0:0:0:1','2026-08-14 15:44:09'),(186,'admin','PASSWORD_RESET','ACCOUNT','katz','重置密码：katz','0:0:0:0:0:0:0:1','2026-08-14 15:45:22'),(187,'admin','AVATAR_UPDATE','ACCOUNT','admin','更新头像：admin','0:0:0:0:0:0:0:1','2026-08-14 15:45:40'),(188,'zhang','AVATAR_UPDATE','ACCOUNT','zhang','更新头像：zhang','0:0:0:0:0:0:0:1','2026-08-14 15:46:31'),(189,'katz','AVATAR_UPDATE','ACCOUNT','katz','更新头像：katz','0:0:0:0:0:0:0:1','2026-08-14 15:47:39'),(190,'admin','CREATE','ANNOUNCEMENT','3','发布公告：E2E 测试公告','0:0:0:0:0:0:0:1','2026-08-14 15:59:55'),(191,'admin','UPDATE','ANNOUNCEMENT','3','下线公告：E2E 测试公告','0:0:0:0:0:0:0:1','2026-08-14 16:00:33'),(192,'admin','CREATE','ANNOUNCEMENT','4','发布公告：E2E 测试公告','0:0:0:0:0:0:0:1','2026-08-14 16:01:55'),(193,'admin','UPDATE','ANNOUNCEMENT','4','下线公告：E2E 测试公告','0:0:0:0:0:0:0:1','2026-08-14 16:01:55'),(194,'admin','UPDATE','ANNOUNCEMENT','4','发布公告：E2E 测试公告','0:0:0:0:0:0:0:1','2026-08-14 16:01:55'),(195,'admin','UPDATE','ANNOUNCEMENT','4','编辑公告：E2E 测试公告（已编辑）','0:0:0:0:0:0:0:1','2026-08-14 16:01:55'),(196,'admin','DELETE','ANNOUNCEMENT','4','删除公告：E2E 测试公告（已编辑）','0:0:0:0:0:0:0:1','2026-08-14 16:01:55'),(197,'admin','DELETE','ANNOUNCEMENT','3','删除公告：E2E 测试公告','0:0:0:0:0:0:0:1','2026-08-14 16:01:55'),(198,'admin','CREATE','ANNOUNCEMENT','5','发布公告：E2E 新闻公告','0:0:0:0:0:0:0:1','2026-08-14 16:13:07'),(199,'admin','CREATE','ANNOUNCEMENT','6','发布公告：E2E 定时公告','0:0:0:0:0:0:0:1','2026-08-14 16:13:07'),(200,'admin','UPDATE','ANNOUNCEMENT','5','编辑公告：E2E 定时公告','0:0:0:0:0:0:0:1','2026-08-14 16:13:07'),(201,'admin','CREATE','ANNOUNCEMENT','7','发布公告：E2E 到期公告','0:0:0:0:0:0:0:1','2026-08-14 16:13:07'),(202,'admin','UPDATE','ANNOUNCEMENT','7','下线公告：E2E 到期公告','0:0:0:0:0:0:0:1','2026-08-14 16:14:22'),(203,'admin','DELETE','ANNOUNCEMENT','7','删除公告：E2E 到期公告','0:0:0:0:0:0:0:1','2026-08-14 16:14:23'),(204,'admin','DELETE','ANNOUNCEMENT','5','删除公告：E2E 定时公告','0:0:0:0:0:0:0:1','2026-08-14 16:14:23'),(205,'admin','CREATE','ANNOUNCEMENT','8','发布公告：E2E 到期重发','0:0:0:0:0:0:0:1','2026-08-14 16:16:12'),(206,'admin','UPDATE','ANNOUNCEMENT','8','重新发布（清除到期时间）：E2E 到期重发','0:0:0:0:0:0:0:1','2026-08-14 16:17:27'),(207,'admin','DELETE','ANNOUNCEMENT','8','删除公告：E2E 到期重发','0:0:0:0:0:0:0:1','2026-08-14 16:17:27'),(208,'admin','DELETE','ANNOUNCEMENT','6','删除公告：E2E 定时公告','0:0:0:0:0:0:0:1','2026-08-14 16:17:28'),(209,'admin','CREATE','ANNOUNCEMENT','9','发布公告：胜多负少','0:0:0:0:0:0:0:1','2026-08-14 16:24:47'),(210,'admin','UPDATE','ANNOUNCEMENT','9','下线公告：胜多负少','0:0:0:0:0:0:0:1','2026-08-14 16:27:46'),(211,'admin','CREATE','ANNOUNCEMENT','10','发布公告：CSRF修复验证','0:0:0:0:0:0:0:1','2026-08-14 16:37:07'),(212,'admin','DELETE','ANNOUNCEMENT','10','删除公告：CSRF修复验证','0:0:0:0:0:0:0:1','2026-08-14 16:37:08'),(213,'zhang','DELETE','FORUM','2','删除帖子：数据结构期末复习问题（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:51'),(214,'admin','DELETE','FORUM','17','删除帖子：分页测试帖 15（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(215,'zhang','DELETE','FORUM','16','删除帖子：分页测试帖 14（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(216,'zhang','DELETE','FORUM','15','删除帖子：分页测试帖 13（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(217,'zhang','DELETE','FORUM','14','删除帖子：分页测试帖 12（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(218,'zhang','DELETE','FORUM','13','删除帖子：分页测试帖 11（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(219,'zhang','DELETE','FORUM','12','删除帖子：分页测试帖 10（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(220,'zhang','DELETE','FORUM','11','删除帖子：分页测试帖 9（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(221,'zhang','DELETE','FORUM','10','删除帖子：分页测试帖 8（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:52'),(222,'zhang','DELETE','FORUM','9','删除帖子：分页测试帖 7（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:53'),(223,'zhang','DELETE','FORUM','8','删除帖子：分页测试帖 6（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:53'),(224,'zhang','DELETE','FORUM','7','删除帖子：分页测试帖 5（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:53'),(225,'zhang','DELETE','FORUM','6','删除帖子：分页测试帖 4（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:53'),(226,'zhang','DELETE','FORUM','5','删除帖子：分页测试帖 3（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:53'),(227,'zhang','DELETE','FORUM','4','删除帖子：分页测试帖 2（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:53'),(228,'zhang','DELETE','FORUM','3','删除帖子：分页测试帖 1（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:54:53'),(229,'zhang','DELETE','FORUM','18','删除帖子：干净删除验证（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:55:09'),(230,'zhang','DELETE','FORUM','1','删除帖子：数据结构期末复习问题（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 05:55:25'),(231,'katz','DELETE','FORUM','9','删除回复（帖子 20，作者 katz）','0:0:0:0:0:0:0:1','2026-08-15 06:10:20'),(232,'katz','DELETE','FORUM','20','删除帖子：二恶热（作者 katz）','0:0:0:0:0:0:0:1','2026-08-15 06:10:35'),(233,'katz','DELETE','FORUM','8','删除回复（帖子 19，作者 katz）','0:0:0:0:0:0:0:1','2026-08-15 06:10:50'),(234,'katz','DELETE','FORUM','6','删除回复（帖子 19，作者 katz）','0:0:0:0:0:0:0:1','2026-08-15 06:10:55'),(235,'katz','DELETE','FORUM','7','删除回复（帖子 19，作者 katz）','0:0:0:0:0:0:0:1','2026-08-15 06:11:01'),(236,'admin','UPDATE','FORUM','21','置顶帖子：红黑树 vs AVL 求助','0:0:0:0:0:0:0:1','2026-08-15 06:17:46'),(237,'admin','UPDATE','FORUM','21','加精帖子：红黑树 vs AVL 求助','0:0:0:0:0:0:0:1','2026-08-15 06:17:46'),(238,'zhang','DELETE','FORUM','21','删除帖子：红黑树 vs AVL 求助（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 06:17:46'),(239,'zhang','DELETE','FORUM','22','删除帖子：点赞与置顶聚焦验证（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 06:18:02'),(240,'zhang','DELETE','FORUM','23','删除帖子：置顶拒绝验证（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 06:18:14'),(241,'admin','UPDATE','FORUM','19','置顶帖子：大师傅似的','0:0:0:0:0:0:0:1','2026-08-15 06:28:41'),(242,'admin','UPDATE','FORUM','19','加精帖子：大师傅似的','0:0:0:0:0:0:0:1','2026-08-15 06:28:44'),(243,'admin','UPDATE','FORUM','24','置顶帖子：二恶热温热','0:0:0:0:0:0:0:1','2026-08-15 06:30:35'),(244,'admin','UPDATE','FORUM','24','加精帖子：二恶热温热','0:0:0:0:0:0:0:1','2026-08-15 06:30:36'),(245,'admin','CREATE','FORUM','CATEGORY_6','新增板块：E2E测试板块','0:0:0:0:0:0:0:1','2026-08-15 06:40:35'),(246,'admin','UPDATE','FORUM','CATEGORY_1','重命名板块：学习交流 → E2E测试板块·改','0:0:0:0:0:0:0:1','2026-08-15 06:40:35'),(247,'admin','UPDATE','FORUM','CATEGORY_1','停用板块：E2E测试板块·改','0:0:0:0:0:0:0:1','2026-08-15 06:40:36'),(248,'zhang','DELETE','FORUM','19','删除帖子：大师傅似的dfdsfd（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 06:41:16'),(249,'zhang','DELETE','FORUM','25','删除帖子：期末复习求助（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 06:41:16'),(250,'admin','UPDATE','FORUM','CATEGORY_1','重命名板块：E2E测试板块·改 → 学习交流','0:0:0:0:0:0:0:1','2026-08-15 06:42:11'),(251,'admin','UPDATE','FORUM','CATEGORY_1','启用板块：学习交流','0:0:0:0:0:0:0:1','2026-08-15 06:42:11'),(252,'admin','UPDATE','FORUM','CATEGORY_6','停用板块：E2E测试板块','0:0:0:0:0:0:0:1','2026-08-15 06:42:11'),(253,'zhang','DELETE','FORUM','26','删除帖子：历史功能验证帖（v2）（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 06:42:11'),(254,'admin','UPDATE','FORUM','CATEGORY_6','启用板块：E2E测试板块','0:0:0:0:0:0:0:1','2026-08-15 06:51:40'),(257,'admin','UPDATE','FORUM','CATEGORY_6','重命名板块：E2E测试板块 → E2E测试板块x','0:0:0:0:0:0:0:1','2026-08-15 07:37:31'),(258,'admin','UPDATE','FORUM','CATEGORY_6','重命名板块：E2E测试板块x → E2E测试板块','0:0:0:0:0:0:0:1','2026-08-15 07:37:31'),(259,'zhang','DELETE','FORUM','27','删除帖子：数据库索引原理探讨（作者 zhang）','0:0:0:0:0:0:0:1','2026-08-15 07:51:44'),(282,'admin','AVATAR_UPDATE','ACCOUNT','admin','更新头像：admin','0:0:0:0:0:0:0:1','2026-08-15 15:47:56'),(283,'katz','AVATAR_UPDATE','ACCOUNT','katz','更新头像：katz','0:0:0:0:0:0:0:1','2026-08-15 16:04:31'),(295,'admin','UPDATE','DEPARTMENT','人工智能','拖拽调整院系层级：父节点=软件工程，同级排序=1','127.0.0.1','2026-08-16 14:58:11'),(296,'admin','UPDATE','COURSE','CS-101','拖拽调整课程所属院系：CS-101 → 软件工程','127.0.0.1','2026-08-16 14:58:11'),(297,'admin','UPDATE','DEPARTMENT','人工智能','拖拽调整院系层级：父节点=Comp. Sci.，同级排序=3','127.0.0.1','2026-08-16 14:58:20'),(298,'admin','UPDATE','COURSE','CS-101','拖拽调整课程所属院系：CS-101 → 计算机科学与技术','127.0.0.1','2026-08-16 14:58:20'),(310,'admin','UPDATE','COURSE','CS-347','拖拽调整课程所属院系：CS-347 → 软件工程','0:0:0:0:0:0:0:1','2026-08-16 15:16:27'),(311,'admin','UPDATE','COURSE','CS-315','拖拽调整课程所属院系：CS-315 → 人工智能','0:0:0:0:0:0:0:1','2026-08-16 15:16:39'),(312,'admin','UPDATE','COURSE','CS-101','拖拽调整课程所属院系：CS-101 → 软件工程','0:0:0:0:0:0:0:1','2026-08-16 15:16:49'),(324,'zhang','AVATAR_UPDATE','ACCOUNT','zhang','更新头像：zhang','0:0:0:0:0:0:0:1','2026-08-16 16:30:48'),(336,'katz','GRADE_UPDATE','GRADE','45678/CS-101/1/Spring/2010','教师 45565 修改成绩：学生 45678，课程 CS-101（班 1，Spring 2010）：B -> A','0:0:0:0:0:0:0:1','2026-08-16 16:48:20'),(489,'admin','DELETE','COURSE','ASYNC-TEST','删除课程：ASYNC-TEST','0:0:0:0:0:0:0:1','2026-08-17 14:57:18'),(501,'admin','CREATE','COURSE','EN-102','新建课程：EN-102 大学英语（Biology，4 学分）','0:0:0:0:0:0:0:1','2026-08-17 15:15:45'),(513,NULL,'UPDATE','ANNOUNCEMENT','9','公告定时发布生效：胜多负少',NULL,'2026-08-17 16:02:00'),(529,'admin','CREATE','ANNOUNCEMENT','13','发布公告：撒旦发射点','0:0:0:0:0:0:0:1','2026-08-17 16:15:14'),(530,NULL,'UPDATE','ANNOUNCEMENT','13','公告定时发布生效：撒旦发射点',NULL,'2026-08-17 16:16:00'),(544,'admin','CREATE','ANNOUNCEMENT','15','发布公告：地方','0:0:0:0:0:0:0:1','2026-08-17 16:30:21'),(545,NULL,'UPDATE','ANNOUNCEMENT','15','公告定时发布生效：地方',NULL,'2026-08-17 16:31:00'),(563,'admin','CREATE','ANNOUNCEMENT','20','发布公告：二二','0:0:0:0:0:0:0:1','2026-08-17 17:35:10'),(564,NULL,'UPDATE','ANNOUNCEMENT','20','公告定时发布生效：二二',NULL,'2026-08-17 17:36:00');
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campus_edge`
--

DROP TABLE IF EXISTS `campus_edge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campus_edge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_place` varchar(20) NOT NULL COMMENT '起点地点编号',
  `to_place` varchar(20) NOT NULL COMMENT '终点地点编号',
  `distance` int NOT NULL COMMENT '步行距离（米）',
  PRIMARY KEY (`id`),
  KEY `idx_from` (`from_place`),
  KEY `idx_to` (`to_place`),
  CONSTRAINT `fk_edge_from` FOREIGN KEY (`from_place`) REFERENCES `campus_place` (`place_id`),
  CONSTRAINT `fk_edge_to` FOREIGN KEY (`to_place`) REFERENCES `campus_place` (`place_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园地点间距离边（无向，双向建图）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campus_edge`
--

LOCK TABLES `campus_edge` WRITE;
/*!40000 ALTER TABLE `campus_edge` DISABLE KEYS */;
INSERT INTO `campus_edge` VALUES (1,'GATE-1','ADMIN-1',220),(2,'GATE-1','DORM-1',350),(3,'GATE-1','BUILD-1',300),(4,'ADMIN-1','BUILD-1',180),(5,'ADMIN-1','DORM-1',260),(6,'DORM-1','CANT-1',150),(7,'DORM-1','LIB-1',380),(8,'BUILD-1','CANT-1',130),(9,'BUILD-1','SCIE-1',220),(10,'CANT-1','LIB-1',200),(11,'CANT-1','BUILD-3',280),(12,'LIB-1','DORM-2',420),(13,'LIB-1','MEDI-1',180),(14,'LIB-1','BUILD-2',240),(15,'MEDI-1','DORM-2',200),(16,'BUILD-2','CANT-2',140),(17,'BUILD-2','GYM-1',220),(18,'DORM-2','CANT-2',160),(19,'CANT-2','DORM-3',120),(20,'DORM-3','GATE-2',200),(21,'DORM-3','GYM-1',300),(22,'BUILD-3','SCIE-1',200),(23,'SCIE-1','GYM-1',320),(24,'GYM-1','GYM-2',380),(25,'BUILD-3','GYM-2',420);
/*!40000 ALTER TABLE `campus_edge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `campus_place`
--

DROP TABLE IF EXISTS `campus_place`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campus_place` (
  `place_id` varchar(20) NOT NULL COMMENT '地点编号（如 LIB-1）',
  `name` varchar(50) NOT NULL COMMENT '地点名称（如 图书馆）',
  `type` varchar(20) NOT NULL DEFAULT 'BUILDING' COMMENT '类型：BUILDING教学楼/DORM宿舍/CANTEEN食堂/LIBRARY图书馆/GYM体育馆/GATE校门/OTHER其他',
  `x` int NOT NULL COMMENT 'SVG 画布 x 坐标（0~1000）',
  `y` int NOT NULL COMMENT 'SVG 画布 y 坐标（0~600）',
  PRIMARY KEY (`place_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园地点（最短路径图的节点）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `campus_place`
--

LOCK TABLES `campus_place` WRITE;
/*!40000 ALTER TABLE `campus_place` DISABLE KEYS */;
INSERT INTO `campus_place` VALUES ('ADMIN-1','行政楼','BUILDING',100,400),('BUILD-1','第一教学楼','BUILDING',260,320),('BUILD-2','第二教学楼','BUILDING',640,380),('BUILD-3','实验楼','BUILDING',420,480),('CANT-1','第一食堂','CANTEEN',320,220),('CANT-2','第二食堂','CANTEEN',720,250),('DORM-1','1号学生宿舍','DORM',180,150),('DORM-2','2号学生宿舍','DORM',800,120),('DORM-3','研究生公寓','DORM',880,320),('GATE-1','南校门','GATE',40,520),('GATE-2','北校门','GATE',940,60),('GYM-1','体育馆','GYM',560,540),('GYM-2','游泳馆','GYM',900,480),('LIB-1','图书馆','LIBRARY',480,150),('MEDI-1','校医院','OTHER',620,90),('SCIE-1','理科实验中心','BUILDING',240,540);
/*!40000 ALTER TABLE `campus_place` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID（自增主键）',
  `from_user` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '发送方登录账号',
  `from_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '发送方显示名（冗余，便于历史展示）',
  `to_user` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收方登录账号',
  `to_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收方显示名（冗余）',
  `content` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `is_compressed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已哈夫曼压缩存档（0/1）',
  `content_huf` mediumblob COMMENT '哈夫曼压缩载荷（树+填充位+编码数据）',
  `read_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读：1 已读 / 0 未读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_chat_pair` (`from_user`,`to_user`,`create_time`),
  KEY `idx_chat_unread` (`to_user`,`read_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内聊天消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_message`
--

LOCK TABLES `chat_message` WRITE;
/*!40000 ALTER TABLE `chat_message` DISABLE KEYS */;
INSERT INTO `chat_message` VALUES (69,'zhang','Zhang','98765','Bourikas','格式发给',0,NULL,0,'2026-08-15 02:34:25'),(70,'zhang','Zhang','98765','Bourikas','十分士大夫士大夫',0,NULL,0,'2026-08-15 02:34:32'),(71,'zhang','Zhang','98765','Bourikas','的风热',0,NULL,0,'2026-08-15 02:34:36'),(72,'zhang','Zhang','98765','Bourikas','发士大夫士大夫',0,NULL,0,'2026-08-15 02:34:42'),(85,'zhang','Zhang','10101','Srinivasan','跨实例聊天测试',0,NULL,0,'2026-08-15 07:17:25'),(86,'zhang','Zhang','10101','Srinivasan','缓存验证消息',0,NULL,0,'2026-08-15 07:37:15'),(87,'admin','管理员','zhang','Zhang','哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长聊天文本，用于展示无损压缩率。数据结构应用：最优前缀码。',1,_binary '\0\0\0�\0\0\0\0\0f�g\0y:\0\0T\�ou(\0\0S�\0Y\'v�\0\0�J�\�\0�\0g�c_\0\0\0\0\0f/g,\0epY+\0\0k�Y\r\0[�~\�\0\0\0cnN�)\0\0^�\0�\Z\0\0\0\0s��\�0\0\0\\UO\0e\�g	\0\0\0N\0�\0�\�Y)\0\0e�RM\0xQ�$`J�n\�8�ucԿ̾Yߐ�\�\�v}+�b�w�;�_m�$�	R�\�n�z���\�;\�:ϥp\�TN\��p\�\��d��*Q�`\�MՏR�2�g~B\�A\�\����ް\�}�\�0%J7lI��\�_\�_,\�\�\\t\�;>�\�1Q;\�ï���F�F탉7V=K�\�\����gҸf*\'zøu\�߲H��(ݰq&\�ǩ�|��!qӠ\��W\�D\�Xw�\�\�I��$\�X\�/\�/�w\�.:t�Jᘨ�\�\�\�\�~\�#T�v�ě���e\�\���\�N��\�\\3�a\�:�o\�$`J�n\�8�ucԿ̾Yߐ�\�\�v}+�b�w�;�_m�$�	R�\�n�z���\�;\�:ϥp\�TN\��p\�\��d',0,'2026-08-16 21:17:31'),(88,'katz','Katz','zhang','Zhang','顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦水水水水水水水水水水水水水水水水水水水反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶',1,_binary '\0\0\0\0\0l4\0\0S\�Y\rTC\0p\��v������\�m�\�m���UUUP\0\0\0\0��UUU����',1,'2026-08-16 21:36:39'),(89,'katz','Katz','zhang','Zhang','反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦日日日日日日日日日日日日日日日日日日日呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦呱呱呱呱呱呱呱呱呱呱呱呱呱呱呱古古怪怪哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈日日日日日日日日日日日日日日日日日日日哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇登山涉水水水涩涩的顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦呱呱呱呱呱呱呱呱呱呱呱呱呱呱呱古古怪怪啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇呱呱呱呱呱呱呱呱呱呱呱呱呱呱呱古古怪怪嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻嘻水水水水水水水水水水水水水水水水水水水哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈',1,_binary '\0\0\0O\0\0\0T\�Tq\0p\�\0l4\0\0`*\0\0\\qv{\0m�\0v�m�\0S\�S\�\0T\�\0\0\0\0Y\r�vTCe\�\0\0V;��UJ}�0I$�I$�\�\�\�\�\�\�\�\�\�\�9\�s�\�9\�s�\�9���������������I$�I$I$�I$��q�\0\0\0\0\0\0\�\�\�\�\�\�\�\�\�\�UUUU\�\��\�{\�\��\�{\�\������WG{3;;;�\�q\�q\�\�\��$�I$�D�I$�Iy\����������������$�I$�\�\�9\�s�\�9\�s�\�9\�q��������������\0\0\0\0\0\0\0\0',1,'2026-08-16 21:47:33');
/*!40000 ALTER TABLE `chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classroom`
--

DROP TABLE IF EXISTS `classroom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classroom` (
  `building` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教学楼',
  `room_number` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '房间号',
  `capacity` int DEFAULT NULL COMMENT '容量',
  PRIMARY KEY (`building`,`room_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教室';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classroom`
--

LOCK TABLES `classroom` WRITE;
/*!40000 ALTER TABLE `classroom` DISABLE KEYS */;
INSERT INTO `classroom` VALUES ('Packard','101',500),('Painter','102',100),('Painter','514',10),('Taylor','3128',70),('Watson','100',30),('Watson','120',50);
/*!40000 ALTER TABLE `classroom` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `course_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程号（主键）',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程名',
  `dept_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属系（FK→department）',
  `credits` decimal(2,0) DEFAULT NULL COMMENT '学分',
  PRIMARY KEY (`course_id`),
  KEY `fk_course_department` (`dept_name`),
  CONSTRAINT `fk_course_department` FOREIGN KEY (`dept_name`) REFERENCES `department` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course`
--

LOCK TABLES `course` WRITE;
/*!40000 ALTER TABLE `course` DISABLE KEYS */;
INSERT INTO `course` VALUES ('BIO-101','Intro. to Biology','生物科学',4),('BIO-301','Genetics','生物科学',4),('BIO-399','Computational Biology','生物科学',3),('CS-101','Intro. to Computer Science','软件工程',4),('CS-190','Game Design','计算机科学与技术',4),('CS-315','Robotics','人工智能',3),('CS-319','Image Processing','计算机科学与技术',3),('CS-347','Database System Concepts','软件工程',3),('EE-181','Intro. to Digital Systems','电子信息工程',3),('EN-101','English','金融学',4),('EN-102','大学英语','Biology',4),('ENG-101','English','金融学',4),('FIN-201','Investment Banking','金融学',3),('HIS-351','World History','历史学',3),('MU-199','Music Video Production','音乐学',3),('PHY-101','Physical Principles','物理学',4);
/*!40000 ALTER TABLE `course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `dept_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '系名称（主键）',
  `parent_name` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父节点（邻接表；NULL=顶级院系）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同级排序（越小越靠前）',
  `building` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所在教学楼',
  `budget` decimal(12,2) DEFAULT NULL COMMENT '经费预算',
  PRIMARY KEY (`dept_name`),
  KEY `idx_department_parent` (`parent_name`),
  CONSTRAINT `fk_department_parent` FOREIGN KEY (`parent_name`) REFERENCES `department` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES ('Biology',NULL,0,'Watson',90000.00),('Comp. Sci.',NULL,0,'Taylor',100000.00),('Elec. Eng.',NULL,0,'Taylor',85000.00),('Finance',NULL,0,'Painter',120000.00),('History',NULL,0,'Painter',50000.00),('Music',NULL,0,'Packard',80000.00),('Physics',NULL,0,'Watson',70000.00),('人工智能','Comp. Sci.',3,'Taylor',35000.00),('历史学','History',1,'Painter',20000.00),('应用物理学','Physics',2,'Watson',26000.00),('物理学','Physics',1,'Watson',30000.00),('生物工程','Biology',2,'Watson',25000.00),('生物科学','Biology',1,'Watson',30000.00),('电子信息工程','Elec. Eng.',1,'Taylor',30000.00),('计算机科学与技术','Comp. Sci.',1,'Taylor',40000.00),('软件工程','Comp. Sci.',2,'Taylor',30000.00),('通信工程','Elec. Eng.',2,'Taylor',28000.00),('金融学','Finance',1,'Painter',40000.00),('音乐学','Music',1,'Packard',30000.00);
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollment_capacity`
--

DROP TABLE IF EXISTS `enrollment_capacity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment_capacity` (
  `course_id` varchar(8) NOT NULL,
  `capacity` int NOT NULL COMMENT '选课容量（人数）',
  `note` varchar(100) DEFAULT NULL COMMENT '说明（如"热门课，容量紧张"）',
  PRIMARY KEY (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程选课容量（最大流建模：课程→汇 容量边）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollment_capacity`
--

LOCK TABLES `enrollment_capacity` WRITE;
/*!40000 ALTER TABLE `enrollment_capacity` DISABLE KEYS */;
INSERT INTO `enrollment_capacity` VALUES ('BIO-101',3,'热门课，容量紧张'),('BIO-301',3,NULL),('BIO-399',4,NULL),('CS-101',3,'热门课，容量紧张'),('CS-190',3,'热门课，容量紧张'),('CS-315',2,'热门课，容量紧张'),('CS-319',2,'热门课，容量紧张'),('CS-347',2,'热门课，容量紧张'),('EE-181',4,NULL),('EN-101',4,NULL),('EN-102',4,NULL),('ENG-101',4,NULL),('FIN-201',4,NULL),('HIS-351',4,NULL),('MU-199',4,NULL),('PHY-101',4,NULL);
/*!40000 ALTER TABLE `enrollment_capacity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollment_wish`
--

DROP TABLE IF EXISTS `enrollment_wish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment_wish` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(5) NOT NULL COMMENT '学生ID（student.ID）',
  `course_id` varchar(8) NOT NULL COMMENT '志愿课程（course.course_id）',
  `priority` int NOT NULL DEFAULT '1' COMMENT '志愿优先级（1 最高）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wish` (`student_id`,`course_id`),
  KEY `idx_wish_course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='选课志愿（最大流建模：学生→课程 需求边）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollment_wish`
--

LOCK TABLES `enrollment_wish` WRITE;
/*!40000 ALTER TABLE `enrollment_wish` DISABLE KEYS */;
INSERT INTO `enrollment_wish` VALUES (1,'00128','CS-101',1),(2,'00128','CS-315',2),(3,'00128','CS-347',3),(4,'00128','BIO-101',4),(5,'12345','CS-101',1),(6,'12345','CS-319',2),(7,'12345','CS-190',3),(8,'19991','HIS-351',1),(9,'19991','MU-199',2),(10,'19991','CS-101',3),(11,'23121','CS-190',1),(12,'23121','BIO-101',2),(13,'23121','EE-181',3),(14,'44553','PHY-101',1),(15,'44553','CS-347',2),(16,'44553','MU-199',3),(17,'45678','BIO-101',1),(18,'45678','CS-101',2),(19,'45678','CS-315',3),(20,'54321','CS-101',1),(21,'54321','CS-190',2),(22,'54321','EE-181',3),(23,'54321','FIN-201',4),(24,'55739','MU-199',1),(25,'55739','HIS-351',2),(26,'55739','CS-319',3),(27,'70557','BIO-301',1),(28,'70557','BIO-101',2),(29,'70557','CS-190',3),(30,'76543','CS-101',1),(31,'76543','CS-315',2),(32,'76543','PHY-101',3),(33,'76653','HIS-351',1),(34,'76653','MU-199',2),(35,'76653','EN-101',3),(36,'96321','CS-101',1),(37,'96321','CS-319',2),(38,'96321','CS-347',3),(39,'96542','BIO-101',1),(40,'96542','CS-190',2),(41,'96542','PHY-101',3),(42,'98765','CS-101',1),(43,'98765','CS-319',2),(44,'98765','BIO-101',3),(45,'98988','BIO-101',1),(46,'98988','PHY-101',2),(47,'98988','CS-347',3),(48,'98988','EN-101',4);
/*!40000 ALTER TABLE `enrollment_wish` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'0','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2026-08-11 17:14:56',0,1),(2,'1','business schema and data','SQL','V1__business_schema_and_data.sql',-816277338,'root','2026-08-11 17:14:56',131,1),(3,'2','auth rbac schema and data','SQL','V2__auth_rbac_schema_and_data.sql',-593448079,'root','2026-08-11 17:14:56',61,1),(4,'3','audit log','SQL','V3__audit_log.sql',1196329791,'root','2026-08-11 17:14:57',9,1),(5,'4','instructor phone number','SQL','V4__instructor_phone_number.sql',-1862808283,'root','2026-08-11 17:14:57',16,1),(6,'5','user avatar','SQL','V5__user_avatar.sql',-1764129103,'root','2026-08-14 07:20:02',43,1),(7,'6','announcement','SQL','V6__announcement.sql',-767316488,'root','2026-08-14 07:58:33',51,1),(8,'7','announcement enhance','SQL','V7__announcement_enhance.sql',287059134,'root','2026-08-14 08:10:43',52,1),(9,'8','chat message','SQL','V8__chat_message.sql',-19802282,'root','2026-08-14 08:56:25',60,1),(10,'9','forum','SQL','V9__forum.sql',-1481224208,'root','2026-08-14 21:53:19',95,1),(11,'10','forum enhance','SQL','V10__forum_enhance.sql',-2016474393,'root','2026-08-14 22:16:32',191,1),(12,'11','forum categories history notify','SQL','V11__forum_categories_history_notify.sql',-1019340710,'root','2026-08-14 22:38:59',246,1),(13,'12','fulltext indexes','SQL','V12__fulltext_indexes.sql',-1665328018,'root','2026-08-14 23:49:01',854,1),(14,'13','department tree','SQL','V13__department_tree.sql',-1408118450,'root','2026-08-16 06:56:57',380,1),(15,'14','demo data','SQL','V14__demo_data.sql',141122032,'root','2026-08-16 09:34:19',130,1),(16,'15','demo grades fix','SQL','V15__demo_grades_fix.sql',-1347699688,'root','2026-08-16 09:37:04',9,1),(17,'16','forum reply tree','SQL','V16__forum_reply_tree.sql',-2145186699,'root','2026-08-16 10:15:35',252,1),(18,'17','huffman compression','SQL','V17__huffman_compression.sql',152790725,'root','2026-08-16 13:16:41',225,1),(19,'18','campus map','SQL','V18__campus_map.sql',-1941800292,'root','2026-08-17 16:21:06',105,1),(20,'19','enrollment maxflow','SQL','V19__enrollment_maxflow.sql',-957663235,'root','2026-08-17 17:12:24',90,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forum_category`
--

DROP TABLE IF EXISTS `forum_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '板块ID（自增主键）',
  `name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '板块名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序（小在前）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用：1 启用 / 0 停用（停用后新帖不可选，存量帖保留）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_category_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='论坛板块（管理员维护）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forum_category`
--

LOCK TABLES `forum_category` WRITE;
/*!40000 ALTER TABLE `forum_category` DISABLE KEYS */;
INSERT INTO `forum_category` VALUES (1,'学习交流',1,1,'2026-08-15 06:38:59'),(2,'课程答疑',2,1,'2026-08-15 06:38:59'),(3,'校园生活',3,1,'2026-08-15 06:38:59'),(4,'资源共享',4,1,'2026-08-15 06:38:59'),(5,'意见建议',5,1,'2026-08-15 06:38:59'),(6,'E2E测试板块',99,1,'2026-08-15 06:40:35');
/*!40000 ALTER TABLE `forum_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forum_like`
--

DROP TABLE IF EXISTS `forum_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_like` (
  `post_id` bigint NOT NULL COMMENT '帖子ID（FK→forum_post）',
  `user_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '点赞用户登录账号',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`post_id`,`user_id`),
  CONSTRAINT `fk_forum_like_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帖子点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forum_like`
--

LOCK TABLES `forum_like` WRITE;
/*!40000 ALTER TABLE `forum_like` DISABLE KEYS */;
INSERT INTO `forum_like` VALUES (24,'admin','2026-08-15 06:30:33'),(24,'zhang','2026-08-16 16:32:59'),(28,'zhang','2026-08-16 16:31:35'),(29,'00128','2026-07-27 17:35:19'),(29,'10001','2026-07-27 17:51:19'),(29,'10002','2026-07-27 17:52:19'),(29,'12345','2026-07-27 17:36:19'),(29,'19991','2026-07-27 17:37:19'),(29,'23121','2026-07-27 17:38:19'),(29,'44553','2026-07-27 17:39:19'),(29,'45678','2026-07-27 17:40:19'),(29,'54321','2026-07-27 17:41:19'),(29,'55739','2026-07-27 17:42:19'),(29,'70557','2026-07-27 17:43:19'),(29,'76543','2026-07-27 17:44:19'),(29,'76653','2026-07-27 17:45:19'),(29,'96321','2026-07-27 17:49:19'),(29,'98765','2026-07-27 17:46:19'),(29,'98877','2026-07-27 17:47:19'),(29,'98988','2026-07-27 17:48:19'),(29,'admin','2026-07-27 17:50:19'),(29,'katz','2026-08-16 19:11:58'),(30,'00128','2026-07-22 17:35:19'),(30,'12345','2026-07-22 17:36:19'),(30,'19991','2026-07-22 17:37:19'),(30,'23121','2026-07-22 17:38:19'),(30,'44553','2026-07-22 17:39:19'),(30,'45678','2026-07-22 17:40:19'),(30,'54321','2026-07-22 17:41:19'),(30,'55739','2026-07-22 17:42:19'),(30,'70557','2026-07-22 17:43:19'),(30,'76543','2026-07-22 17:44:19'),(30,'76653','2026-07-22 17:45:19'),(30,'98765','2026-07-22 17:46:19'),(31,'00128','2026-08-04 17:35:19'),(31,'12345','2026-08-04 17:36:19'),(31,'19991','2026-08-04 17:37:19'),(31,'23121','2026-08-04 17:38:19'),(31,'44553','2026-08-04 17:39:19'),(31,'45678','2026-08-04 17:40:19'),(31,'54321','2026-08-04 17:41:19'),(31,'55739','2026-08-04 17:42:19'),(32,'00128','2026-08-08 17:35:19'),(32,'12345','2026-08-08 17:36:19'),(32,'19991','2026-08-08 17:37:19'),(32,'23121','2026-08-08 17:38:19'),(32,'44553','2026-08-08 17:39:19'),(32,'45678','2026-08-08 17:40:19'),(32,'54321','2026-08-08 17:41:19'),(32,'55739','2026-08-08 17:42:19'),(32,'70557','2026-08-08 17:43:19'),(32,'76543','2026-08-08 17:44:19'),(32,'76653','2026-08-08 17:45:19'),(32,'96321','2026-08-08 17:49:19'),(32,'98765','2026-08-08 17:46:19'),(32,'98877','2026-08-08 17:47:19'),(32,'98988','2026-08-08 17:48:19'),(33,'00128','2026-08-06 17:35:19'),(33,'12345','2026-08-06 17:36:19'),(33,'19991','2026-08-06 17:37:19'),(33,'23121','2026-08-06 17:38:19'),(33,'44553','2026-08-06 17:39:19'),(33,'45678','2026-08-06 17:40:19'),(34,'00128','2026-08-01 17:35:19'),(34,'12345','2026-08-01 17:36:19'),(34,'19991','2026-08-01 17:37:19'),(34,'23121','2026-08-01 17:38:19'),(34,'44553','2026-08-01 17:39:19'),(34,'45678','2026-08-01 17:40:19'),(34,'54321','2026-08-01 17:41:19'),(34,'55739','2026-08-01 17:42:19'),(34,'70557','2026-08-01 17:43:19'),(35,'00128','2026-08-10 17:35:19'),(35,'12345','2026-08-10 17:36:19'),(35,'19991','2026-08-10 17:37:19'),(35,'23121','2026-08-10 17:38:19'),(35,'44553','2026-08-10 17:39:19'),(35,'45678','2026-08-10 17:40:19'),(35,'54321','2026-08-10 17:41:19'),(36,'00128','2026-07-29 17:35:19'),(36,'12345','2026-07-29 17:36:19'),(36,'19991','2026-07-29 17:37:19'),(36,'23121','2026-07-29 17:38:19'),(36,'44553','2026-07-29 17:39:19'),(36,'45678','2026-07-29 17:40:19'),(36,'54321','2026-07-29 17:41:19'),(36,'55739','2026-07-29 17:42:19'),(36,'70557','2026-07-29 17:43:19'),(36,'76543','2026-07-29 17:44:19'),(37,'00128','2026-07-25 17:35:19'),(37,'12345','2026-07-25 17:36:19'),(37,'19991','2026-07-25 17:37:19'),(37,'23121','2026-07-25 17:38:19'),(37,'44553','2026-07-25 17:39:19'),(38,'00128','2026-07-19 17:35:19'),(38,'12345','2026-07-19 17:36:19'),(38,'19991','2026-07-19 17:37:19'),(38,'23121','2026-07-19 17:38:19'),(38,'44553','2026-07-19 17:39:19'),(38,'45678','2026-07-19 17:40:19'),(38,'54321','2026-07-19 17:41:19'),(38,'55739','2026-07-19 17:42:19'),(38,'70557','2026-07-19 17:43:19'),(38,'76543','2026-07-19 17:44:19'),(38,'76653','2026-07-19 17:45:19'),(39,'00128','2026-08-07 17:35:19'),(39,'12345','2026-08-07 17:36:19'),(39,'19991','2026-08-07 17:37:19'),(39,'23121','2026-08-07 17:38:19'),(39,'44553','2026-08-07 17:39:19'),(39,'45678','2026-08-07 17:40:19'),(40,'00128','2026-07-31 17:35:19'),(40,'12345','2026-07-31 17:36:19'),(40,'19991','2026-07-31 17:37:19'),(40,'23121','2026-07-31 17:38:19'),(40,'44553','2026-07-31 17:39:19'),(40,'45678','2026-07-31 17:40:19'),(40,'54321','2026-07-31 17:41:19'),(40,'55739','2026-07-31 17:42:19'),(40,'70557','2026-07-31 17:43:19'),(41,'00128','2026-08-09 17:35:19'),(41,'12345','2026-08-09 17:36:19'),(41,'19991','2026-08-09 17:37:19'),(41,'23121','2026-08-09 17:38:19'),(42,'00128','2026-08-11 17:35:19'),(42,'12345','2026-08-11 17:36:19'),(42,'19991','2026-08-11 17:37:19'),(42,'23121','2026-08-11 17:38:19'),(42,'44553','2026-08-11 17:39:19'),(42,'45678','2026-08-11 17:40:19'),(42,'54321','2026-08-11 17:41:19'),(42,'55739','2026-08-11 17:42:19'),(42,'70557','2026-08-11 17:43:19'),(42,'76543','2026-08-11 17:44:19'),(42,'76653','2026-08-11 17:45:19'),(42,'98765','2026-08-11 17:46:19'),(42,'98877','2026-08-11 17:47:19'),(43,'00128','2026-08-05 17:35:19'),(43,'12345','2026-08-05 17:36:19'),(43,'19991','2026-08-05 17:37:19'),(43,'23121','2026-08-05 17:38:19'),(43,'44553','2026-08-05 17:39:19'),(43,'45678','2026-08-05 17:40:19'),(43,'54321','2026-08-05 17:41:19'),(43,'55739','2026-08-05 17:42:19'),(44,'00128','2026-08-12 17:35:19'),(44,'12345','2026-08-12 17:36:19'),(44,'19991','2026-08-12 17:37:19'),(45,'00128','2026-08-13 17:35:19'),(45,'12345','2026-08-13 17:36:19'),(45,'19991','2026-08-13 17:37:19'),(45,'23121','2026-08-13 17:38:19'),(45,'44553','2026-08-13 17:39:19'),(45,'45678','2026-08-13 17:40:19'),(45,'54321','2026-08-13 17:41:19');
/*!40000 ALTER TABLE `forum_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forum_post`
--

DROP TABLE IF EXISTS `forum_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID（自增主键）',
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '正文',
  `content_huf` mediumblob COMMENT '哈夫曼压缩载荷（树+填充位+编码数据）',
  `category_id` int DEFAULT NULL COMMENT '板块ID（FK→forum_category）',
  `pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '置顶：1 置顶（管理员）',
  `featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '加精：1 精华（管理员）',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数（冗余计数）',
  `author_user` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者登录账号',
  `author_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者显示名（冗余）',
  `reply_count` int NOT NULL DEFAULT '0' COMMENT '回复数（冗余计数）',
  `last_reply_time` datetime DEFAULT NULL COMMENT '最后回复时间（列表按活跃度排序）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_forum_post_active` (`last_reply_time`,`id`),
  FULLTEXT KEY `ft_forum_post_title_content` (`title`,`content`) /*!50100 WITH PARSER `ngram` */ 
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='论坛帖子';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forum_post`
--

LOCK TABLES `forum_post` WRITE;
/*!40000 ALTER TABLE `forum_post` DISABLE KEYS */;
INSERT INTO `forum_post` VALUES (24,'二恶热温热','石帆胜丰士大夫士大夫的地方的士大夫士大夫士大夫士大夫发顺丰到付的',NULL,1,1,1,2,'admin','管理员',3,'2026-08-16 16:33:19','2026-08-15 06:30:14','2026-08-16 16:33:18'),(28,'为热热','二二委任为染色法打发打发士大夫的从v从v从',NULL,3,0,0,1,'zhang','Zhang',1,'2026-08-16 16:32:07','2026-08-16 16:31:15','2026-08-16 16:32:06'),(29,'数据库原理及应用','把数据库范式这部分整理了一下，3NF 和 BCNF 的区别欢迎大家讨论，期末考高频。',NULL,2,1,0,19,'00128','Zhang',16,'2026-08-16 19:02:49','2026-07-27 17:34:19','2026-08-16 19:11:58'),(30,'数据库系统概念学习笔记','跟着教材过了一遍《数据库系统概念》，把索引、事务、并发控制的重点摘出来了。',NULL,1,0,0,12,'12345','Shankar',9,'2026-08-16 18:55:33','2026-07-22 17:34:19','2026-08-16 18:55:33'),(31,'数据库课程设计答疑','课程设计选题定了图书管理系统，E-R 图转关系模式时遇到问题，求指点。',NULL,2,0,0,8,'44553','Peltier',9,'2026-08-10 17:34:19','2026-08-04 17:34:19','2026-08-16 17:34:19'),(32,'数据库期末复习重点整理','整理了一份期末复习提纲：SQL 聚合查询、范式判定、事务隔离级别，附例题。',NULL,1,0,0,15,'23121','Chavez',5,'2026-08-12 17:34:19','2026-08-08 17:34:19','2026-08-16 17:34:19'),(33,'数据结构期末考试求助','图的遍历和最短路径这块总记不住，哪位同学有通俗易懂的总结？',NULL,2,0,0,6,'54321','Williams',12,'2026-08-11 17:34:19','2026-08-06 17:34:19','2026-08-16 17:34:19'),(34,'数据结构课程设计报告模板','分享一份课程设计报告模板，含目录结构、时间安排和答辩要点，直接可用。',NULL,4,0,0,9,'45678','Levy',4,'2026-08-09 05:34:19','2026-08-01 17:34:19','2026-08-16 17:34:19'),(35,'数据结构复习提纲','栈、队列、树、图、排序、查找一章一节列了重点，供大家查漏补缺。',NULL,1,0,0,7,'55739','Sanchez',3,'2026-08-13 17:34:19','2026-08-10 17:34:19','2026-08-16 17:34:19'),(36,'计算机网络实验指导','Wireshark 抓包实验的完整步骤和常见问题整理，抓不到包的同学看这里。',NULL,4,0,0,10,'70557','Snow',7,'2026-08-07 17:34:19','2026-07-29 17:34:19','2026-08-16 17:34:19'),(37,'计算机组成原理重点总结','指令流水线、Cache 映射方式、中断处理流程，考前速记版。',NULL,1,0,0,5,'76543','Brown',2,'2026-08-05 17:34:19','2026-07-25 17:34:19','2026-08-16 17:34:19'),(38,'操作系统原理学习笔记','进程调度算法、死锁避免、虚拟内存，结合教材整理了这份笔记。',NULL,1,0,0,11,'76653','Aoi',8,'2026-08-02 17:34:19','2026-07-19 17:34:19','2026-08-16 17:34:19'),(39,'操作系统实验环境搭建教程','虚拟机装 Linux 跑实验的保姆级教程，含常见报错解决，环境搭不起来的看这篇。',NULL,4,0,0,6,'98765','Bourikas',5,'2026-08-12 05:34:19','2026-08-07 17:34:19','2026-08-16 17:34:19'),(40,'人工智能导论读书笔记','搜索算法、机器学习基础、神经网络入门，一章一章过完的笔记。',NULL,1,0,0,9,'98877','Kapoor',6,'2026-08-08 17:34:19','2026-07-31 17:34:19','2026-08-16 17:34:19'),(41,'人工智能期末作业思路分享','决策树分类作业的思路和踩坑记录，数据集预处理很关键。',NULL,2,0,0,4,'98988','Tanaka',3,'2026-08-13 05:34:19','2026-08-09 17:34:19','2026-08-16 17:34:19'),(42,'软件工程实践心得体会','这学期小组项目从需求分析到部署上线全流程复盘，需求变更真的是最大的坑。',NULL,1,0,1,13,'96321','wangwu',9,'2026-08-14 05:34:19','2026-08-11 17:34:19','2026-08-16 17:34:19'),(43,'软件测试入门资源汇总','单元测试、集成测试、测试用例设计方法的学习资料链接整理，自取。',NULL,4,0,0,8,'00128','Zhang',4,'2026-08-11 05:34:19','2026-08-05 17:34:19','2026-08-16 17:34:19'),(44,'数学建模竞赛组队邀请','国赛组队还差一人，会 Python 和 LaTeX 的同学看过来，队友已就位。',NULL,3,0,0,3,'12345','Shankar',11,'2026-08-14 17:34:19','2026-08-12 17:34:19','2026-08-16 17:34:19'),(45,'Java 并发编程实战笔记','线程池参数、synchronized 与 Lock、CAS 原理，实战项目踩坑笔记。',NULL,1,0,0,7,'23121','Chavez',5,'2026-08-15 05:34:19','2026-08-13 17:34:19','2026-08-16 17:34:19'),(46,'让他人托人','@00128 打发士大夫温热反对反对发士大夫士大夫士大夫士大夫十分士大夫打发士大夫地方是的发射点反对反对发发士大夫士大夫士大夫发射点反对发射点发射点发发士大夫地方',NULL,5,0,0,0,'katz','Katz',3,'2026-08-16 18:41:12','2026-08-16 18:36:12','2026-08-16 18:41:11'),(47,'通知测试-账号提及','@zhang 这是账号形式的提及测试',NULL,1,0,0,0,'katz','Katz',0,NULL,'2026-08-16 18:47:53','2026-08-16 18:47:53'),(48,'通知测试-业务号提及','@00128 这是业务号形式的提及测试',NULL,1,0,0,0,'katz','Katz',0,NULL,'2026-08-16 18:47:53','2026-08-16 18:47:53'),(49,'哈夫曼压缩存档演示帖','哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。哈夫曼压缩演示：这是一段有大量重复内容的长文本，用于展示无损压缩率。数据结构应用：最优前缀码。',_binary '\0\0\0�\0\0\0\0\0epRM\0[�Y+\0\0�\�s�\0cnf/\0\0\0e�N�\0k�g,\0\0N\0�y:\0\0\0\0�\�\0g��\0\0e\�v�\0�\�c_\0\0\0~\�f�\0^�\\U\0\0Y\'\0�\Z\0\0\0)\0oT\�\0u(0\0\0\0g\0x\0Q�Y\r\0\0g	OS�\0\�t�=\�;儇}\nV�q\�5���\�\rDUj�zy\�c��\�\�1د,$;\�R��I�\��.�j\"�W\�\�\�\�\�?\�Oy�\�ya!\�B��\\zMo$��vQZ��\�w�\��2{\�v+\�	��B\�\�ky\'��\Z��\��\�6\�\�O\�\�c�^XHwХj�[\�?\�]�\�EV�ǡ��\�:��\��\�\�C��+P�\��\�I��\��*�~=\r�\�1\��d\��\�W�\�)Z�Ǥ\�\�O�`5U�\�\�m\�y��\�\'�\�b���\�J\�.=&��\����_�Co;',1,0,0,0,'admin','管理员',0,NULL,'2026-08-16 21:16:57','2026-08-16 21:16:57'),(50,'而尔特瑞特让他','而额我热温热温热温热十分士大夫士大夫士大夫try同意同意同意干活干活干活干活打发打发打发打发从v从v从v从v打发打发打发打发热热热热饿饿饿饿饿饿俄而日日日日日日日顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃日日日日日日日日日日日日日日日日日日日水水水水水水水水水水水水水水水水水水水哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇',_binary '\0\0\0o\0\0\0TCl4�v\0\0\0\0\0Y+T\0n)X\�\0�\0Y\'\0\0b\0r\0\0tO\�e\�\0\0\0S\�bS\0\0^rm;\0\0vN\�\0\0p\�\0\0�\0SAR\0\0��\0yaT\�\�\�9ȹ\"\�W\\sA\�G4ӽ�\���=\�\�Mt\�Ms�\�8\�7\�}�\�}��\�8\�9\�rR��)��\�mUUUUUUUUUP\0\0\0\0\0\0�\�m�\�m��I$�I$����������\�',1,0,0,0,'katz','Katz',0,NULL,'2026-08-16 21:26:28','2026-08-16 21:26:28');
/*!40000 ALTER TABLE `forum_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forum_post_history`
--

DROP TABLE IF EXISTS `forum_post_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_post_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史ID（自增主键）',
  `post_id` bigint NOT NULL COMMENT '帖子ID（FK→forum_post，级联删除）',
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '编辑前标题',
  `content` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '编辑前正文',
  `category_id` int DEFAULT NULL COMMENT '编辑前板块',
  `edited_by` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '编辑者登录账号',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  PRIMARY KEY (`id`),
  KEY `idx_fph_post` (`post_id`,`id`),
  CONSTRAINT `fk_fph_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帖子编辑历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forum_post_history`
--

LOCK TABLES `forum_post_history` WRITE;
/*!40000 ALTER TABLE `forum_post_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `forum_post_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forum_reply`
--

DROP TABLE IF EXISTS `forum_reply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forum_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回复ID（自增主键）',
  `post_id` bigint NOT NULL COMMENT '所属帖子（FK→forum_post）',
  `parent_id` bigint DEFAULT NULL COMMENT '父回复ID（邻接表；NULL=楼回复）',
  `content` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '回复内容',
  `author_user` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者登录账号',
  `author_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '作者显示名（冗余）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
  PRIMARY KEY (`id`),
  KEY `idx_forum_reply_post` (`post_id`,`id`),
  KEY `idx_forum_reply_parent` (`parent_id`),
  CONSTRAINT `fk_forum_reply_parent` FOREIGN KEY (`parent_id`) REFERENCES `forum_reply` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forum_reply_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=189 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='论坛回复';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forum_reply`
--

LOCK TABLES `forum_reply` WRITE;
/*!40000 ALTER TABLE `forum_reply` DISABLE KEYS */;
INSERT INTO `forum_reply` VALUES (18,24,NULL,'撒发射点发射点发射点特瑞特瑞特让他体育与与','admin','管理员','2026-08-15 06:30:47'),(19,24,NULL,'@zhang dfsdfsdfsdserererfesfgfdfdfsdfd','admin','管理员','2026-08-15 06:52:24'),(20,28,NULL,'法沙发沙发','zhang','Zhang','2026-08-16 16:32:06'),(21,24,NULL,'收到方式的风热发射点发射点反对','zhang','Zhang','2026-08-16 16:33:18'),(22,29,NULL,'同问，蹲一个答案','00128','Zhang','2026-07-27 18:34:19'),(23,29,NULL,'感谢分享，很有帮助','12345','Shankar','2026-07-27 19:34:19'),(24,29,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-07-27 20:34:19'),(25,29,NULL,'我也遇到了同样的问题','23121','Chavez','2026-07-27 21:34:19'),(26,29,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-07-27 22:34:19'),(27,29,NULL,'讲得很清楚，收藏了','45678','Levy','2026-07-27 23:34:19'),(28,29,NULL,'补充一点：记得先看官方文档','54321','Williams','2026-07-28 00:34:19'),(29,29,NULL,'支持一下，顶','55739','Sanchez','2026-07-28 01:34:19'),(30,29,NULL,'mark，回头细看','70557','Snow','2026-07-28 02:34:19'),(31,29,NULL,'赞，写得很用心','76543','Brown','2026-07-28 03:34:19'),(37,30,NULL,'同问，蹲一个答案','00128','Zhang','2026-07-22 18:34:19'),(38,30,NULL,'感谢分享，很有帮助','12345','Shankar','2026-07-22 19:34:19'),(39,30,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-07-22 20:34:19'),(40,30,NULL,'我也遇到了同样的问题','23121','Chavez','2026-07-22 21:34:19'),(41,30,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-07-22 22:34:19'),(42,30,NULL,'讲得很清楚，收藏了','45678','Levy','2026-07-22 23:34:19'),(44,31,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-04 18:34:19'),(45,31,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-04 19:34:19'),(46,31,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-04 20:34:19'),(47,31,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-04 21:34:19'),(48,31,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-08-04 22:34:19'),(49,31,NULL,'讲得很清楚，收藏了','45678','Levy','2026-08-04 23:34:19'),(50,31,NULL,'补充一点：记得先看官方文档','54321','Williams','2026-08-05 00:34:19'),(51,31,NULL,'支持一下，顶','55739','Sanchez','2026-08-05 01:34:19'),(52,31,NULL,'mark，回头细看','70557','Snow','2026-08-05 02:34:19'),(59,32,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-08 18:34:19'),(60,32,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-08 19:34:19'),(61,32,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-08 20:34:19'),(62,32,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-08 21:34:19'),(63,32,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-08-08 22:34:19'),(66,33,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-06 18:34:19'),(67,33,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-06 19:34:19'),(68,33,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-06 20:34:19'),(69,33,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-06 21:34:19'),(70,33,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-08-06 22:34:19'),(71,33,NULL,'讲得很清楚，收藏了','45678','Levy','2026-08-06 23:34:19'),(72,33,NULL,'补充一点：记得先看官方文档','54321','Williams','2026-08-07 00:34:19'),(73,33,NULL,'支持一下，顶','55739','Sanchez','2026-08-07 01:34:19'),(74,33,NULL,'mark，回头细看','70557','Snow','2026-08-07 02:34:19'),(75,33,NULL,'赞，写得很用心','76543','Brown','2026-08-07 03:34:19'),(76,33,NULL,'这个思路不错，学习了','76653','Aoi','2026-08-07 04:34:19'),(77,33,NULL,'正好需要，谢谢','98765','Bourikas','2026-08-07 05:34:19'),(81,34,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-01 18:34:19'),(82,34,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-01 19:34:19'),(83,34,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-01 20:34:19'),(84,34,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-01 21:34:19'),(88,35,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-10 18:34:19'),(89,35,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-10 19:34:19'),(90,35,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-10 20:34:19'),(91,36,NULL,'同问，蹲一个答案','00128','Zhang','2026-07-29 18:34:19'),(92,36,NULL,'感谢分享，很有帮助','12345','Shankar','2026-07-29 19:34:19'),(93,36,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-07-29 20:34:19'),(94,36,NULL,'我也遇到了同样的问题','23121','Chavez','2026-07-29 21:34:19'),(95,36,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-07-29 22:34:19'),(96,36,NULL,'讲得很清楚，收藏了','45678','Levy','2026-07-29 23:34:19'),(97,36,NULL,'补充一点：记得先看官方文档','54321','Williams','2026-07-30 00:34:19'),(98,37,NULL,'感谢分享，很有帮助','12345','Shankar','2026-07-25 19:34:19'),(99,37,NULL,'同问，蹲一个答案','00128','Zhang','2026-07-25 18:34:19'),(101,38,NULL,'同问，蹲一个答案','00128','Zhang','2026-07-19 18:34:19'),(102,38,NULL,'感谢分享，很有帮助','12345','Shankar','2026-07-19 19:34:19'),(103,38,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-07-19 20:34:19'),(104,38,NULL,'我也遇到了同样的问题','23121','Chavez','2026-07-19 21:34:19'),(105,38,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-07-19 22:34:19'),(106,38,NULL,'讲得很清楚，收藏了','45678','Levy','2026-07-19 23:34:19'),(107,38,NULL,'补充一点：记得先看官方文档','54321','Williams','2026-07-20 00:34:19'),(108,38,NULL,'支持一下，顶','55739','Sanchez','2026-07-20 01:34:19'),(116,39,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-08-07 22:34:19'),(117,39,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-07 21:34:19'),(118,39,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-07 20:34:19'),(119,39,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-07 19:34:19'),(120,39,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-07 18:34:19'),(123,40,NULL,'讲得很清楚，收藏了','45678','Levy','2026-07-31 23:34:19'),(124,40,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-07-31 22:34:19'),(125,40,NULL,'我也遇到了同样的问题','23121','Chavez','2026-07-31 21:34:19'),(126,40,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-07-31 20:34:19'),(127,40,NULL,'感谢分享，很有帮助','12345','Shankar','2026-07-31 19:34:19'),(128,40,NULL,'同问，蹲一个答案','00128','Zhang','2026-07-31 18:34:19'),(130,41,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-09 20:34:19'),(131,41,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-09 19:34:19'),(132,41,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-09 18:34:19'),(133,42,NULL,'mark，回头细看','70557','Snow','2026-08-12 02:34:19'),(134,42,NULL,'支持一下，顶','55739','Sanchez','2026-08-12 01:34:19'),(135,42,NULL,'补充一点：记得先看官方文档','54321','Williams','2026-08-12 00:34:19'),(136,42,NULL,'讲得很清楚，收藏了','45678','Levy','2026-08-11 23:34:19'),(137,42,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-08-11 22:34:19'),(138,42,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-11 21:34:19'),(139,42,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-11 20:34:19'),(140,42,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-11 19:34:19'),(141,42,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-11 18:34:19'),(148,43,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-05 21:34:19'),(149,43,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-05 20:34:19'),(150,43,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-05 19:34:19'),(151,43,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-05 18:34:19'),(155,44,NULL,'这个思路不错，学习了','76653','Aoi','2026-08-13 04:34:19'),(156,44,NULL,'赞，写得很用心','76543','Brown','2026-08-13 03:34:19'),(157,44,NULL,'mark，回头细看','70557','Snow','2026-08-13 02:34:19'),(158,44,NULL,'支持一下，顶','55739','Sanchez','2026-08-13 01:34:19'),(159,44,NULL,'补充一点：记得先看官方文档','54321','Williams','2026-08-13 00:34:19'),(160,44,NULL,'讲得很清楚，收藏了','45678','Levy','2026-08-12 23:34:19'),(161,44,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-08-12 22:34:19'),(162,44,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-12 21:34:19'),(163,44,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-12 20:34:19'),(164,44,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-12 19:34:19'),(165,44,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-12 18:34:19'),(170,45,NULL,'已解决，参考楼上同学的方案','44553','Peltier','2026-08-13 22:34:19'),(171,45,NULL,'我也遇到了同样的问题','23121','Chavez','2026-08-13 21:34:19'),(172,45,NULL,'这里有个坑，大家注意下','19991','Brandt','2026-08-13 20:34:19'),(173,45,NULL,'感谢分享，很有帮助','12345','Shankar','2026-08-13 19:34:19'),(174,45,NULL,'同问，蹲一个答案','00128','Zhang','2026-08-13 18:34:19'),(177,29,22,'@00128 楼中回复测试：树形结构展示','admin','管理员','2026-08-16 18:16:17'),(178,29,22,'@00128 二恶热打发打发','katz','Katz','2026-08-16 18:22:02'),(179,29,177,'@admin 二二二为热热打发士大夫士大夫','katz','Katz','2026-08-16 18:22:23'),(180,29,177,'石帆胜丰士大夫士大夫','katz','Katz','2026-08-16 18:23:17'),(181,46,NULL,'@10211 撒旦发射点发射点发射点发射点二二二二\r\n时发生飞洒发生发生的反对发射点发生的热','katz','Katz','2026-08-16 18:37:18'),(182,46,181,'@katz 十分士大夫地方豆腐干恢复供电和热热热热热','katz','Katz','2026-08-16 18:37:41'),(183,46,NULL,'@00128 十分士大夫士大夫士大夫是','katz','Katz','2026-08-16 18:41:11'),(184,30,37,'@00128 发生发射点反对','zhang','Zhang','2026-08-16 18:55:14'),(185,30,38,'@12345 反对发射点发射点','zhang','Zhang','2026-08-16 18:55:21'),(186,30,39,'@19991 石帆胜丰士大夫士大夫士大夫撒旦','zhang','Zhang','2026-08-16 18:55:33'),(187,29,NULL,'@zhang sdfsdfsdfsdfsdfsdfsdfsddf','katz','Katz','2026-08-16 19:01:39'),(188,29,NULL,'@katz dfsfsfsafsdfserererere','zhang','Zhang','2026-08-16 19:02:48');
/*!40000 ALTER TABLE `forum_reply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instructor`
--

DROP TABLE IF EXISTS `instructor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `instructor` (
  `ID` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师编号（主键）',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `dept_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属系（FK→department）',
  `salary` decimal(8,2) DEFAULT NULL COMMENT '工资',
  `phone_number` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `fk_instructor_department` (`dept_name`),
  FULLTEXT KEY `ft_instructor_name` (`name`) /*!50100 WITH PARSER `ngram` */ ,
  CONSTRAINT `fk_instructor_department` FOREIGN KEY (`dept_name`) REFERENCES `department` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instructor`
--

LOCK TABLES `instructor` WRITE;
/*!40000 ALTER TABLE `instructor` DISABLE KEYS */;
INSERT INTO `instructor` VALUES ('10101','Srinivasan','Comp. Sci.',68250.00,'15928446010'),('10211','Smith','Biology',69300.00,'15928446010'),('12121','Wu','Finance',94500.00,'15928446010'),('15151','Mozart','Music',42000.00,'15928446010'),('22222','Einstein','Physics',99750.00,'15928446010'),('32343','El Said','History',63000.00,'15928446010'),('33456','Gold','Physics',91350.00,'15928446010'),('45565','Katz','Comp. Sci.',78750.00,'15928446010'),('45678','zhangshan','Biology',98755.00,NULL),('58583','Califieri','History',65100.00,'15928446010'),('76543','Singh','Finance',84000.00,'15928446010'),('76766','Crick','Biology',75600.00,'15928446010'),('83821','Brandt','Comp. Sci.',96600.00,'15928446010'),('98345','Kim','Elec. Eng.',84000.00,'15928446010'),('98765','WeiFeng','Comp. Sci.',10000.00,'15928446010');
/*!40000 ALTER TABLE `instructor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `prereq`
--

DROP TABLE IF EXISTS `prereq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prereq` (
  `course_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程号（FK→course）',
  `prereq_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '先修课程号（FK→course）',
  PRIMARY KEY (`course_id`,`prereq_id`),
  KEY `fk_prereq_course_prereq` (`prereq_id`),
  CONSTRAINT `fk_prereq_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `fk_prereq_course_prereq` FOREIGN KEY (`prereq_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='先修课';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prereq`
--

LOCK TABLES `prereq` WRITE;
/*!40000 ALTER TABLE `prereq` DISABLE KEYS */;
INSERT INTO `prereq` VALUES ('BIO-301','BIO-101'),('CS-190','CS-101'),('CS-315','CS-101'),('CS-319','CS-101'),('CS-347','CS-101'),('BIO-399','CS-190'),('BIO-101','CS-315'),('EE-181','PHY-101');
/*!40000 ALTER TABLE `prereq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `section`
--

DROP TABLE IF EXISTS `section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `section` (
  `course_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程号（FK→course）',
  `sec_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '开课号',
  `semester` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学期（Fall/Spring/Summer）',
  `year` smallint NOT NULL COMMENT '年份',
  `building` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教学楼（FK→classroom）',
  `room_number` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '房间号（FK→classroom）',
  `time_slot_id` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时间段标识',
  PRIMARY KEY (`course_id`,`sec_id`,`semester`,`year`),
  KEY `fk_section_classroom` (`building`,`room_number`),
  CONSTRAINT `fk_section_classroom` FOREIGN KEY (`building`, `room_number`) REFERENCES `classroom` (`building`, `room_number`),
  CONSTRAINT `fk_section_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开课班';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `section`
--

LOCK TABLES `section` WRITE;
/*!40000 ALTER TABLE `section` DISABLE KEYS */;
INSERT INTO `section` VALUES ('BIO-101','1','Summer',2009,'Painter','514','B'),('BIO-101','2','Fall',2010,'Packard','101','B'),('BIO-101','2','Fall',2011,'Painter','102','D'),('BIO-101','3','Spring',2010,'Painter','514','A'),('BIO-101','3','Spring',2011,'Taylor','3128','G'),('BIO-301','1','Spring',2011,'Painter','102','D'),('BIO-301','1','Summer',2010,'Painter','514','A'),('CS-101','1','Fall',2009,'Packard','101','H'),('CS-101','1','Spring',2010,'Watson','120','G'),('CS-190','1','Spring',2009,'Taylor','3128','E'),('CS-190','2','Spring',2009,'Taylor','3128','A'),('CS-315','1','Spring',2010,'Painter','514','G'),('CS-319','1','Spring',2010,'Painter','102','D'),('CS-319','2','Spring',2010,'Painter','514','C'),('CS-347','1','Fall',2009,'Taylor','3128','A'),('EE-181','1','Spring',2009,'Taylor','3128','C'),('EN-101','1','Spring',2010,'Painter','102','A'),('EN-101','2','Spring',2010,'Watson','120','B'),('ENG-101','2','Spring',2010,'Packard','101','C'),('FIN-201','1','Spring',2010,'Packard','101','A'),('HIS-351','1','Spring',2010,'Taylor','3128','C'),('HIS-351','2','Fall',2010,'Watson','120','A'),('MU-199','1','Spring',2010,'Packard','101','H'),('MU-199','2','Spring',2010,'Taylor','3128','A'),('PHY-101','1','Fall',2009,'Watson','100','A'),('PHY-101','1','Spring',2010,'Watson','120','F');
/*!40000 ALTER TABLE `section` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `ID` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生编号（主键）',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `dept_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主修系（FK→department）',
  `tot_cred` int DEFAULT NULL COMMENT '已修总学分',
  PRIMARY KEY (`ID`),
  KEY `fk_student_department` (`dept_name`),
  FULLTEXT KEY `ft_student_name` (`name`) /*!50100 WITH PARSER `ngram` */ ,
  CONSTRAINT `fk_student_department` FOREIGN KEY (`dept_name`) REFERENCES `department` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student`
--

LOCK TABLES `student` WRITE;
/*!40000 ALTER TABLE `student` DISABLE KEYS */;
INSERT INTO `student` VALUES ('00128','Zhang','Comp. Sci.',14),('12345','Shankar','Comp. Sci.',14),('19991','Brandt','History',3),('23121','Chavez','Finance',3),('44553','Peltier','Physics',4),('45678','Levy','Physics',7),('54321','Williams','Comp. Sci.',8),('55739','Sanchez','Music',3),('70557','Snow','Physics',4),('76543','Brown','Comp. Sci.',7),('76653','Aoi','Elec. Eng.',3),('96321','wangwu','Biology',4),('96542','lisi','Biology',NULL),('98765','Bourikas','Elec. Eng.',7),('98988','Tanaka','Biology',4);
/*!40000 ALTER TABLE `student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_announcement`
--

DROP TABLE IF EXISTS `sys_announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_announcement` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID（自增主键）',
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告内容',
  `category` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE 通知 / NEWS 新闻动态 / ACTIVITY 活动 / OTHER 其他',
  `pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '置顶：1 置顶 / 0 普通',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '发布状态：1 已发布（对外可见）/ 0 已下线',
  `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间（NULL=立即发布，未来时间则到时自动对外可见）',
  `expire_time` datetime DEFAULT NULL COMMENT '到期时间（NULL=永不过期），到期自动下线',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统公告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_announcement`
--

LOCK TABLES `sys_announcement` WRITE;
/*!40000 ALTER TABLE `sys_announcement` DISABLE KEYS */;
INSERT INTO `sys_announcement` VALUES (1,'演示账号说明','演示账号：zhang（学生）/ katz（教师）/ admin（管理员），密码均为 password。','NOTICE',1,1,NULL,NULL,'2026-08-14 15:58:33','2026-08-14 15:58:33'),(2,'访问建议','建议使用 Chrome / Edge 浏览器访问本站，以获得最佳体验。','NOTICE',0,1,NULL,NULL,'2026-08-14 15:58:33','2026-08-14 15:58:33'),(3,'新学期选课即将开始','新学期选课将于近期开放，请同学们提前规划课程，详见学生中心\"选课 / 退课\"。','NEWS',0,1,NULL,NULL,'2026-08-14 16:10:43','2026-08-14 16:10:43'),(9,'胜多负少','手动阀手动阀士大夫萨芬士大夫发士大夫虽然微软士大夫士大夫士大夫','NOTICE',0,1,'2026-08-14 16:26:00',NULL,'2026-08-14 16:24:47','2026-08-17 16:02:00'),(13,'撒旦发射点','石帆胜丰士大夫反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦呱呱呱呱呱呱呱呱呱呱呱呱呱呱呱古古怪怪踩踩踩踩踩踩踩踩踩踩踩踩踩踩踩踩踩踩从','OTHER',1,1,'2026-08-17 16:16:00',NULL,'2026-08-17 16:15:14','2026-08-17 16:16:00'),(15,'地方','沙发沙发发石帆胜丰士大夫士大夫的','OTHER',1,1,'2026-08-17 16:31:00',NULL,'2026-08-17 16:30:21','2026-08-17 16:31:00'),(20,'二二','反对法烦烦烦烦烦烦烦烦烦反反复复烦烦烦呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃呃哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇哇钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱钱','NEWS',1,1,'2026-08-17 17:36:00',NULL,'2026-08-17 17:35:10','2026-08-17 17:36:00');
/*!40000 ALTER TABLE `sys_announcement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `permission_id` int NOT NULL AUTO_INCREMENT COMMENT '权限ID（自增主键）',
  `perm_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限编码（如 course:view）',
  `perm_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限说明',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`perm_code`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='权限';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permission`
--

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1,'department:view','查看系','浏览系列表与详情'),(2,'department:manage','管理系','系的新增/修改/删除'),(3,'course:view','查看课程','浏览课程目录与详情'),(4,'course:manage','管理课程','课程的新增/修改/删除'),(5,'instructor:view','查看教师','浏览教师列表与详情'),(6,'instructor:manage','管理教师','教师的新增/修改/删除'),(7,'student:view','查看学生','浏览学生信息'),(8,'student:manage','管理学生','学生的新增/修改/删除'),(9,'section:view','查看开课班','浏览开课班信息'),(10,'section:manage','排课管理','开课班的新增/修改/删除'),(11,'classroom:manage','管理教室','教室的新增/修改/删除'),(12,'prereq:manage','管理先修','先修关系的新增/删除'),(13,'take:enroll','选课/退课','学生选课与退课'),(14,'take:grade','成绩录入','教师录入/修改成绩'),(15,'take:transcript','成绩单','查看本人成绩单'),(16,'advisor:view','导师查询','查看导师信息'),(17,'stats:view','统计报表','查看统计报表'),(18,'user:manage','用户管理','账号与权限管理');
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色标识（主键）：STUDENT / INSTRUCTOR / ADMIN',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色说明',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES ('ADMIN','管理员','维护基础数据、分配账号与权限、查看统计报表'),('INSTRUCTOR','教师','查看授课任务、班级名单、录入/修改成绩'),('STUDENT','学生','浏览课程、选课/退课、查看成绩单与导师');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `role_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色（FK->sys_role）',
  `permission_id` int NOT NULL COMMENT '权限（FK->sys_permission）',
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `fk_role_perm_perm` (`permission_id`),
  CONSTRAINT `fk_role_perm_perm` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`permission_id`),
  CONSTRAINT `fk_role_perm_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色-权限关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES ('ADMIN',1),('INSTRUCTOR',1),('STUDENT',1),('ADMIN',2),('ADMIN',3),('INSTRUCTOR',3),('STUDENT',3),('ADMIN',4),('ADMIN',5),('INSTRUCTOR',5),('STUDENT',5),('ADMIN',6),('ADMIN',7),('INSTRUCTOR',7),('STUDENT',7),('ADMIN',8),('ADMIN',9),('INSTRUCTOR',9),('STUDENT',9),('ADMIN',10),('ADMIN',11),('ADMIN',12),('ADMIN',13),('STUDENT',13),('ADMIN',14),('INSTRUCTOR',14),('ADMIN',15),('STUDENT',15),('ADMIN',16),('STUDENT',16),('ADMIN',17),('ADMIN',18);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号（主键）',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（BCrypt 哈希，不存明文）',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联类型：STUDENT / INSTRUCTOR / ADMIN',
  `ref_id` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联业务主键：student.ID / instructor.ID；ADMIN 为 NULL',
  `avatar` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像文件名（存储在 uploads 目录，仅存文件名）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1 启用 / 0 禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统账号';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES ('10101','$2a$10$DPI8pRcvZTNzEZhjheRYCeYwPn4sw79EP1V8OhQj3p.jGqCJkghuO','INSTRUCTOR','10101',NULL,1,'2026-08-11 15:01:52','2026-08-12 00:42:36'),('10211','$2a$10$GwJ55TdoN9YWTqZu4RttDuE/qkonXup.MwFn53XGsmwkDcWYRMP3G','INSTRUCTOR','10211',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('12121','$2a$10$.BwcAvAHBi92CoCgR5QU6OgP3J.66.FYeGC5VnmaQZavkqbO8u50S','INSTRUCTOR','12121',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('15151','$2a$10$DmFB1Pg81BBJgrsd1U.bpuu45rkbY7XVG9cLSdE84QGC75KioxY9C','INSTRUCTOR','15151',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('19991','$2a$10$lO/F9Wlkek2etKgpTaO3Vu12j2JSzIsMIyDDE5xFgeDR/ZGK7SQDa','STUDENT','19991',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('22222','$2a$10$Mn84Vx6voOWDuYhpsv.dL.pzk/SJB1ui01B116PPgbryKn87fkKoi','INSTRUCTOR','22222',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('23121','$2a$10$0F2VfS5.Rgb3OVUEYaQpOuf/FOsaaRbW8fvPlroa42QJ66AejVTQi','STUDENT','23121',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('32343','$2a$10$hMx999DvhqdkUkLBVfcRhOI8CSVFFq9DSD0wcwLg2HivvMIJg.8Ui','INSTRUCTOR','32343',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('33456','$2a$10$AlFFY5p0Dce12Bo6SB2TF./DtBVf1MtAL2I7mgSbHaJj/boMWIKUu','INSTRUCTOR','33456',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('44553','$2a$10$fKhdZ9SEJ3Thn8VVkaPsW.FxkWpVRal29ltgLaiRjwvJI/1mE9EOq','STUDENT','44553',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('45678','$2a$10$VLoSb4.vMeUTeeRwnGJyI.W1aZw1c09dIBkiEEINtSzFmwifbIBpS','STUDENT','45678',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('54321','$2a$10$3F/3QVinkKAwD5oUNQBLsOs0WWPEXArgf4S5XZosJMvhleVLXIVg6','STUDENT','54321',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('55739','$2a$10$7F..V2Y06/SpPOyTgjIt.u.j0BW4wbQx46GrMtb0jF0MVjsi6ZnyW','STUDENT','55739',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('58583','$2a$10$qF9Jg3PNfgqaDTD0a2hMt.AN3eRjgrVG4T6jczpEFDJxzAFp8C6W2','INSTRUCTOR','58583',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('70557','$2a$10$QDjQNZ5ZrMKp7xJV5EAo3eW7Umf55yfQ457lgtT3yrt/E/.HJG2E6','STUDENT','70557',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('76543','$2a$10$Z6o3zyWEnoX3Aq2zrToGe.SHHU/7eeANrTdjzYZerLTTeoXoN5VHW','STUDENT','76543',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('76653','$2a$10$NGqV1O58ND.c3MwIEeP4sOMVX30YAUNYuuVPHnvA1aBKxHt7IF4aC','STUDENT','76653',NULL,1,'2026-08-11 15:01:51','2026-08-11 15:01:51'),('76766','$2a$10$ruQBKh7D844uQdm5GE/1te.UDikK09ZVfI267t9547W/vbU0iUOxa','INSTRUCTOR','76766',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('83821','$2a$10$zuXPZmPr.1H55GcQkgkW/.x/AYmpvZ3x9au6T48FUuILB86uTjf7O','INSTRUCTOR','83821',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('96321','$2a$10$gUI5p6UfkHVxT8PNow1WbOLrgMtUj3dSIEPI1cZXoddEBMHQFz6YC','STUDENT','96321',NULL,1,'2026-08-11 14:56:02','2026-08-11 14:56:02'),('96542','$2a$10$lYhnba4vUq3HhGa0liKmFeZtjc7xYrHnJROOmLhvDOzS8lVkQUpCO','STUDENT','96542',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('98345','$2a$10$HyhXXLKLyStyVjMIAtFd6.TZ9NyqrXPnbxSr3ig/DUwSaIUbXTtsa','INSTRUCTOR','98345',NULL,0,'2026-08-11 15:01:53','2026-08-12 00:42:53'),('98765','$2a$10$P5EWwdyDhXDLSOsfKsr6D.XZqJC2GsHWMYz7/SqjCPGQMigNTSgOe','STUDENT','98765',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('98988','$2a$10$rnaZHd.hJWqlLcsj7Ns4RuO19znlvefJeVwaa4zPmcoApscc9gnKa','STUDENT','98988',NULL,1,'2026-08-11 15:01:52','2026-08-11 15:01:52'),('admin','$2a$10$/Gu.uRug7LYoOu0PzCdVKOqo4Ayxt3fM2utBEet4jNQ5nouNqojKO','ADMIN',NULL,'426a9382123e4b349c2184d915ee53b9.png',1,'2026-08-10 13:48:01','2026-08-15 15:47:56'),('katz','$2a$10$Bw3QSKkP7lFokSX0KoG7Xu5ffJfDbR3jhD71OYd8DU1y966lAUWcS','INSTRUCTOR','45565','fe18701d28394e2ca746c10a99ee41d8.png',1,'2026-08-10 13:48:01','2026-08-15 16:04:31'),('test','$2a$10$R4aFo7byvjb28z9t59/fuO08nl17qtgzZ5NDSi3/xfWL0C2zNLbQi','STUDENT','12345',NULL,1,'2026-08-11 14:28:57','2026-08-11 14:37:41'),('zhang','$2a$10$UtAcExBEKPOy9m99YzxkR.0ccJNw2ZOpTN1c53niDThzjnuQc593a','STUDENT','00128','ee447971d3de49ab857643ff53fbb292.png',1,'2026-08-10 13:48:01','2026-08-16 16:30:48');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号（FK->sys_user）',
  `role_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色（FK->sys_role）',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_user_role_role` (`role_id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户-角色关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES ('admin','ADMIN'),('10101','INSTRUCTOR'),('10211','INSTRUCTOR'),('12121','INSTRUCTOR'),('15151','INSTRUCTOR'),('22222','INSTRUCTOR'),('32343','INSTRUCTOR'),('33456','INSTRUCTOR'),('58583','INSTRUCTOR'),('76766','INSTRUCTOR'),('83821','INSTRUCTOR'),('98345','INSTRUCTOR'),('katz','INSTRUCTOR'),('19991','STUDENT'),('23121','STUDENT'),('44553','STUDENT'),('45678','STUDENT'),('54321','STUDENT'),('55739','STUDENT'),('70557','STUDENT'),('76543','STUDENT'),('76653','STUDENT'),('96321','STUDENT'),('96542','STUDENT'),('98765','STUDENT'),('98988','STUDENT'),('test','STUDENT'),('zhang','STUDENT');
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `takes`
--

DROP TABLE IF EXISTS `takes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `takes` (
  `ID` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生编号（FK→student）',
  `course_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程号',
  `sec_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '开课号',
  `semester` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学期',
  `year` smallint NOT NULL COMMENT '年份',
  `grade` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成绩（A+/A-/B/...）',
  PRIMARY KEY (`ID`,`course_id`,`sec_id`,`semester`,`year`),
  KEY `fk_takes_section` (`course_id`,`sec_id`,`semester`,`year`),
  CONSTRAINT `fk_takes_section` FOREIGN KEY (`course_id`, `sec_id`, `semester`, `year`) REFERENCES `section` (`course_id`, `sec_id`, `semester`, `year`),
  CONSTRAINT `fk_takes_student` FOREIGN KEY (`ID`) REFERENCES `student` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `takes`
--

LOCK TABLES `takes` WRITE;
/*!40000 ALTER TABLE `takes` DISABLE KEYS */;
INSERT INTO `takes` VALUES ('00128','CS-101','1','Fall',2009,'A'),('00128','CS-101','1','Spring',2010,'A+'),('00128','CS-315','1','Spring',2010,'A-'),('00128','CS-347','1','Fall',2009,'A-'),('00128','MU-199','1','Spring',2010,NULL),('12345','BIO-101','2','Fall',2011,'B+'),('12345','BIO-101','3','Spring',2010,NULL),('12345','CS-101','1','Fall',2009,'C'),('12345','CS-101','1','Spring',2010,'A-'),('12345','CS-190','2','Spring',2009,'A'),('12345','CS-315','1','Spring',2010,'A'),('12345','CS-319','2','Spring',2010,NULL),('12345','CS-347','1','Fall',2009,'A'),('19991','CS-315','1','Spring',2010,'B+'),('19991','CS-319','1','Spring',2010,'A-'),('19991','HIS-351','1','Spring',2010,'B'),('23121','BIO-101','3','Spring',2010,'B+'),('23121','CS-319','1','Spring',2010,'A'),('23121','FIN-201','1','Spring',2010,'C+'),('44553','BIO-101','2','Fall',2010,'C+'),('44553','CS-319','2','Spring',2010,'B'),('44553','PHY-101','1','Fall',2009,'B-'),('45678','BIO-101','2','Fall',2010,'B'),('45678','BIO-101','3','Spring',2011,'A+'),('45678','CS-101','1','Fall',2009,'F'),('45678','CS-101','1','Spring',2010,'A'),('45678','CS-319','1','Spring',2010,'C'),('54321','BIO-101','3','Spring',2011,'C'),('54321','CS-101','1','Fall',2009,'A-'),('54321','CS-190','2','Spring',2009,'B+'),('54321','CS-315','1','Spring',2010,'C+'),('55739','BIO-101','2','Fall',2010,'A-'),('55739','BIO-301','1','Spring',2011,'B'),('55739','MU-199','1','Spring',2010,'A-'),('76543','BIO-101','2','Fall',2010,'B-'),('76543','BIO-101','2','Fall',2011,'A'),('76543','CS-101','1','Fall',2009,'A+'),('76543','CS-319','2','Spring',2010,'A'),('76653','BIO-101','3','Spring',2011,'B'),('76653','EE-181','1','Spring',2009,'C'),('76653','HIS-351','2','Fall',2010,'C+'),('96321','CS-101','1','Spring',2010,'A'),('96321','CS-319','2','Spring',2010,'A-'),('96321','FIN-201','1','Spring',2010,NULL),('96321','HIS-351','1','Spring',2010,NULL),('98765','BIO-101','2','Fall',2011,'B'),('98765','CS-101','1','Fall',2009,'C-'),('98765','CS-315','1','Spring',2010,'B'),('98765','HIS-351','2','Fall',2010,'A'),('98988','BIO-101','1','Summer',2009,'A'),('98988','BIO-101','2','Fall',2010,'A'),('98988','BIO-101','3','Spring',2010,'A+'),('98988','BIO-301','1','Summer',2010,NULL);
/*!40000 ALTER TABLE `takes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teaches`
--

DROP TABLE IF EXISTS `teaches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaches` (
  `ID` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师编号（FK→instructor）',
  `course_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程号',
  `sec_id` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '开课号',
  `semester` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学期',
  `year` smallint NOT NULL COMMENT '年份',
  PRIMARY KEY (`ID`,`course_id`,`sec_id`,`semester`,`year`),
  KEY `fk_teaches_section` (`course_id`,`sec_id`,`semester`,`year`),
  CONSTRAINT `fk_teaches_instructor` FOREIGN KEY (`ID`) REFERENCES `instructor` (`ID`),
  CONSTRAINT `fk_teaches_section` FOREIGN KEY (`course_id`, `sec_id`, `semester`, `year`) REFERENCES `section` (`course_id`, `sec_id`, `semester`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='授课';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teaches`
--

LOCK TABLES `teaches` WRITE;
/*!40000 ALTER TABLE `teaches` DISABLE KEYS */;
INSERT INTO `teaches` VALUES ('76766','BIO-101','1','Summer',2009),('10211','BIO-101','2','Fall',2010),('98765','BIO-101','2','Fall',2011),('45678','BIO-101','3','Spring',2011),('98765','BIO-301','1','Spring',2011),('76766','BIO-301','1','Summer',2010),('10101','CS-101','1','Fall',2009),('45565','CS-101','1','Spring',2010),('83821','CS-190','1','Spring',2009),('83821','CS-190','2','Spring',2009),('10101','CS-315','1','Spring',2010),('45565','CS-319','1','Spring',2010),('83821','CS-319','2','Spring',2010),('10101','CS-347','1','Fall',2009),('98345','EE-181','1','Spring',2009),('98345','EN-101','1','Spring',2010),('98765','EN-101','2','Spring',2010),('98345','ENG-101','2','Spring',2010),('12121','FIN-201','1','Spring',2010),('32343','HIS-351','1','Spring',2010),('98765','HIS-351','2','Fall',2010),('15151','MU-199','1','Spring',2010),('22222','PHY-101','1','Fall',2009),('98765','PHY-101','1','Spring',2010);
/*!40000 ALTER TABLE `teaches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `time_slot`
--

DROP TABLE IF EXISTS `time_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_slot` (
  `time_slot_id` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `day` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time DEFAULT NULL,
  PRIMARY KEY (`time_slot_id`,`day`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='time_slot';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `time_slot`
--

LOCK TABLES `time_slot` WRITE;
/*!40000 ALTER TABLE `time_slot` DISABLE KEYS */;
INSERT INTO `time_slot` VALUES ('A','F','09:00:00','09:50:00'),('A','M','08:00:00','08:50:00'),('A','W','08:00:00','08:50:00'),('B','F','09:00:00','09:50:00'),('B','M','09:00:00','09:50:00'),('B','W','09:00:00','09:50:00'),('C','F','11:00:00','11:50:00'),('C','M','11:00:00','11:50:00'),('C','W','11:00:00','11:50:00'),('D','M','13:00:00','13:50:00'),('D','W','13:00:00','13:50:00'),('F','F','11:00:00','11:50:00'),('F','M','10:00:00','10:50:00'),('F','W','10:00:00','10:50:00'),('G','F','16:00:00','16:50:00'),('G','M','16:00:00','16:50:00'),('G','W','16:00:00','16:50:00'),('H','W','10:00:00','10:50:00');
/*!40000 ALTER TABLE `time_slot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_notification`
--

DROP TABLE IF EXISTS `user_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID（自增主键）',
  `user_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收人登录账号',
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'FORUM_MENTION' COMMENT '通知类型：FORUM_MENTION 论坛提及',
  `source_url` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '跳转地址（如 /forum/123#reply-45）',
  `summary` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '摘要文案',
  `read_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读：1 已读 / 0 未读',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_user` (`user_id`,`read_flag`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内通知';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_notification`
--

LOCK TABLES `user_notification` WRITE;
/*!40000 ALTER TABLE `user_notification` DISABLE KEYS */;
INSERT INTO `user_notification` VALUES (1,'10101','FORUM_MENTION','/forum/25','Zhang 在帖子《期末复习求助》中提到了你',1,'2026-08-15 06:40:35'),(2,'zhang','FORUM_MENTION','/forum/24#reply-19','管理员 在帖子《二恶热温热》的回复中提到了你',1,'2026-08-15 06:52:24'),(3,'admin','FORUM_MENTION','/forum/29#reply-179','Katz 在帖子《数据库原理及应用》的回复中提到了你',0,'2026-08-16 18:22:23'),(4,'10211','FORUM_MENTION','/forum/46#reply-181','Katz 在帖子《让他人托人》的回复中提到了你',0,'2026-08-16 18:37:18'),(5,'zhang','FORUM_MENTION','/forum/47','Katz 在帖子《通知测试-账号提及》中提到了你',1,'2026-08-16 18:47:53'),(6,'zhang','FORUM_MENTION','/forum/48','Katz 在帖子《通知测试-业务号提及》中提到了你',1,'2026-08-16 18:47:53'),(7,'test','FORUM_MENTION','/forum/30#reply-185','Zhang 在帖子《数据库系统概念学习笔记》的回复中提到了你',0,'2026-08-16 18:55:21'),(8,'19991','FORUM_MENTION','/forum/30#reply-186','Zhang 在帖子《数据库系统概念学习笔记》的回复中提到了你',0,'2026-08-16 18:55:33'),(9,'zhang','FORUM_MENTION','/forum/29#reply-187','Katz 在帖子《数据库原理及应用》的回复中提到了你',1,'2026-08-16 19:01:39'),(10,'katz','FORUM_MENTION','/forum/29#reply-188','Zhang 在帖子《数据库原理及应用》的回复中提到了你',1,'2026-08-16 19:02:48');
/*!40000 ALTER TABLE `user_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'university'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-28 22:25:32
