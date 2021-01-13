
# Dump of table address
# ------------------------------------------------------------

DROP TABLE IF EXISTS `address`;

CREATE TABLE `address` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'address id',
  `name` varchar(256) NOT NULL DEFAULT '' COMMENT 'address name',
  `zip_code` varchar(64) NOT NULL DEFAULT '' COMMENT 'zip code',
  `region_name` varchar(256) NOT NULL DEFAULT '' COMMENT 'region name',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `address` WRITE;
/*!40000 ALTER TABLE `address` DISABLE KEYS */;

INSERT INTO `address` (`id`, `name`, `zip_code`, `region_name`, `create_time`, `update_time`)
VALUES
	(1,'Address_01','214314','RC','2019-01-14 00:00:00','2019-01-14 00:00:00'),
	(2,'Address_02','100100','RA','2019-01-14 00:00:00','2019-01-14 00:00:00'),
	(3,'Address_03','100100','RA','2019-01-14 00:00:00','2019-01-14 00:00:00'),
	(4,'Address_04','100100','RA','2019-01-14 00:00:00','2019-01-14 00:00:00');

/*!40000 ALTER TABLE `address` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table region
# ------------------------------------------------------------

DROP TABLE IF EXISTS `region`;

CREATE TABLE `region` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(256) NOT NULL DEFAULT '' COMMENT 'region name',
  `zip_code` varchar(64) NOT NULL DEFAULT '' COMMENT 'zip code',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `udx_zipcode` (`zip_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `region` WRITE;
/*!40000 ALTER TABLE `region` DISABLE KEYS */;

INSERT INTO `region` (`id`, `name`, `zip_code`, `create_time`, `update_time`)
VALUES
	(1,'RA','100100','2019-01-01 00:00:00','2019-01-01 00:00:00'),
	(2,'RB','211011','2019-01-02 00:00:00','2019-01-02 00:00:00'),
	(3,'RC','214314','2019-01-02 00:00:00','2019-01-02 00:00:00');

/*!40000 ALTER TABLE `region` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table user
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'user id',
  `email` varchar(256) NOT NULL DEFAULT '' COMMENT 'user email',
  `name` varchar(64) NOT NULL DEFAULT '' COMMENT 'user name',
  `age` int(11) NOT NULL COMMENT 'user age',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `udx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;

INSERT INTO `user` (`id`, `email`, `name`, `age`, `create_time`, `update_time`)
VALUES
	(1,'userA@userA.com','userA',10,'2019-01-01 00:00:00','2019-01-01 00:00:00'),
	(2,'userB@userB.com','userB',15,'2019-01-10 00:00:00','2019-01-10 00:00:00'),
	(3,'userC@userC.com','userC',30,'2019-02-03 00:00:00','2019-02-03 00:00:00');

/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table user_address
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_address`;

CREATE TABLE `user_address` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `addr_id` bigint(20) NOT NULL COMMENT 'address id',
  `user_name` varchar(64) NOT NULL DEFAULT '' COMMENT 'user name',
  `addr_name` varchar(256) NOT NULL DEFAULT '' COMMENT 'address name',
  `region_name` varchar(256) NOT NULL DEFAULT '' COMMENT 'region name',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `user_address` WRITE;
/*!40000 ALTER TABLE `user_address` DISABLE KEYS */;

INSERT INTO `user_address` (`id`, `user_id`, `addr_id`, `user_name`, `addr_name`, `region_name`, `create_time`, `update_time`)
VALUES
	(1,2,1,'userB','Address_01','RC','2019-05-01 00:00:00','2019-05-01 00:00:00'),
	(2,3,2,'userC','Address_02','RA','2019-06-02 00:00:00','2019-06-02 00:00:00');

/*!40000 ALTER TABLE `user_address` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table user_message
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_message`;

CREATE TABLE `user_message` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'message id',
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `user_name` varchar(64) NOT NULL DEFAULT '' COMMENT 'user name',
  `message` varchar(512) NOT NULL DEFAULT '' COMMENT 'message',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `user_message` WRITE;
/*!40000 ALTER TABLE `user_message` DISABLE KEYS */;

INSERT INTO `user_message` (`id`, `user_id`, `user_name`, `message`, `create_time`, `update_time`)
VALUES
	(1,1,'userA','hello world.','2019-08-01 00:00:00','2019-08-01 00:00:00'),
	(2,1,'userA','Nice to meet you.','2019-08-02 00:00:00','2019-08-02 00:00:00'),
	(3,2,'userB','I am here.','2019-08-05 00:00:00','2019-08-05 00:00:00');

/*!40000 ALTER TABLE `user_message` ENABLE KEYS */;
UNLOCK TABLES;

